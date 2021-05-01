package me.towdium.jecalculation.utils;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import me.towdium.jecalculation.JustEnoughCalculation;
import me.towdium.jecalculation.polyfill.NBTHelper;
import me.towdium.jecalculation.utils.wrappers.Pair;
import me.towdium.jecalculation.utils.wrappers.Wrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import org.apache.commons.lang3.StringEscapeUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.text.BreakIterator;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Author: Towdium
 * Date:   2016/6/25.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
@ParametersAreNonnullByDefault
public class Utilities {
    // FLOAT FORMATTING
    public static String cutNumber(float f, int size) {
        BiFunction<Float, Integer, String> form = (fl, len) -> {
            String ret = Float.toString(fl);
            if (ret.endsWith(".0"))
                ret = ret.substring(0, ret.length() - 2);
            if (ret.length() > len)
                ret = ret.substring(0, len);
            return ret;
        };
        int scale = (int) Math.log10(f) / 3;
        switch (scale) {
            case 0:
                return form.apply(f, size);
            case 1:
                return form.apply(f / 1000.0f, size - 1) + 'K';
            case 2:
                return form.apply(f / 1000000.0f, size - 1) + 'M';
            case 3:
                return form.apply(f / 1000000000.0f, size - 1) + 'B';
            case 4:
                return form.apply(f / 1000000000000.0f, size - 1) + 'G';
            default:
                return form.apply(f / 1000000000000000.0f, size - 1) + 'T';
        }
    }


    // MOD NAME
    @Nullable
    public static String getModName(Item item) {
        String name = GameData.getItemRegistry().getNameForObject(item);
        String id = name.substring(0, name.indexOf(":"));
        return id.equals("minecraft") ? "Minecraft" : Loader.instance().getIndexedModList().get(id).getName();
    }

    public static String getModName(Fluid fluid) {
        String name = fluid.getName();
        if (name.equals("lava") || name.equals("water"))
            return "Minecraft";
        else
            return Loader.instance().getIndexedModList().get(fluid.getStillIcon().getIconName().split(":")[0])
                         .getName();
    }

    public static NBTTagCompound getTag(ItemStack is) {
        return NBTHelper.getOrCreateSubCompound(is, JustEnoughCalculation.Reference.MODID);
    }

    public static class Timer {
        long time = System.currentTimeMillis();
        boolean running = false;

        public void setState(boolean b) {
            if (!b && running)
                running = false;
            if (b && !running) {
                running = true;
                time = System.currentTimeMillis();
            }
        }

        public long getTime() {
            return running ? System.currentTimeMillis() - time : 0;
        }

    }

    @SuppressWarnings("UnusedReturnValue")
    public static class Circulator {
        int total, current;

        public Circulator(int total) {
            this(total, 0);
        }

        public Circulator(int total, int current) {
            this.total = total;
            this.current = current;
        }

        public int next() {
            return current + 1 == total ? 0 : current + 1;
        }


        public int prev() {
            return current == 0 ? total - 1 : current - 1;
        }

        public Circulator move(int steps) {
            current += steps;
            if (current < 0)
                current += (-current) / total * total + total;
            else
                current = current % total;
            return this;
        }

        public int current() {
            return current;
        }

        public Circulator set(int index) {
            if (index >= 0 && index < total)
                current = index;
            else
                throw new RuntimeException(String.format("Expected: [0, %d), given: %d.", total, index));
            return this;
        }

        public Circulator copy() {
            return new Circulator(total).set(current);
        }
    }

    public static class ReversedIterator<T> implements Iterator<T> {
        ListIterator<T> i;

        public ReversedIterator(List<T> l) {
            i = l.listIterator(l.size());
        }

        public ReversedIterator(ListIterator<T> i) {
            while (i.hasNext())
                i.next();
            this.i = i;
        }

        @Override
        public boolean hasNext() {
            return i.hasPrevious();
        }

        @Override
        public T next() {
            return i.previous();
        }

        public Stream<T> stream() {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
        }
    }

    @SideOnly(Side.CLIENT)
    public static class I18n {
        public static boolean contains(String s1, String s2) {
            return s1.contains(s2);
        }

        public static Pair<String, Boolean> search(String translateKey, Object... parameters) {
            Pair<String, Boolean> ret = new Pair<>(null, null);
            translateKey = "jecharacters." + translateKey;
            String buffer = net.minecraft.client.resources.I18n.format(translateKey, parameters);
            ret.two = !buffer.equals(translateKey);
            buffer = StringEscapeUtils.unescapeJava(buffer);
            ret.one = buffer.replace("\t", "    ");
            return ret;
        }

        public static String format(String translateKey, Object... parameters) {
            return search(translateKey, parameters).one;
        }

        public static List<String> wrap(String s, int width) {
            return new TextWrapper().wrap(s, Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode(),
                                          i -> TextWrapper.renderer.getCharWidth(i), width);
        }

        static class TextWrapper {
            static FontRenderer renderer = Minecraft.getMinecraft().fontRenderer;

            String str;
            BreakIterator it;
            List<String> temp = new ArrayList<>();
            Function<Character, Integer> func;
            int start, end, section, space, cursor, width;

            private void cut() {
                char c = str.charAt(cursor);
                if (c == '\f') cursor++;
                temp.add(str.substring(start, cursor));
                if (c == ' ' || c == '　' || c == '\n') cursor++;
                start = cursor;
                end = cursor;
                space = width;
                section = func.apply(str.charAt(cursor));
            }

            private void move() {
                temp.add(str.substring(start, end));
                start = end;
                space = width;
            }

            private List<String> wrap(String s, String languageCode, Function<Character, Integer> func, int width) {
                Locale l = getLocaleFromString(languageCode);
                temp.clear();
                start = 0;
                end = 0;
                cursor = 0;
                space = width;
                str = s;
                it = BreakIterator.getLineInstance(l);
                it.setText(s);
                this.width = width;
                this.func = func;
                for (int i = it.next(); i != BreakIterator.DONE; i = it.next()) {
                    for (cursor = end; cursor < i; cursor++) {
                        char ch = str.charAt(cursor);
                        section += func.apply(str.charAt(cursor));
                        if (ch == '\n' || ch == '\f') cut();
                        else if (section > space) {
                            if (start == end) cut();
                            else move();
                        }
                    }
                    space -= section;
                    section = 0;
                    end = cursor;
                }
                move();
                return temp;
            }
        }
    }

    public static class Recent<T> {
        LinkedList<T> data = new LinkedList<>();
        BiPredicate<T, T> tester;
        int limit;

        public Recent(BiPredicate<T, T> tester, int limit) {
            this.tester = tester;
            this.limit = limit;
        }

        public Recent(int limit) {
            this.limit = limit;
        }

        public void push(T obj) {
            data.removeIf(t -> tester != null ? tester.test(t, obj) : t.equals(obj));
            data.push(obj);
            if (data.size() > limit)
                data.pop();
        }

        public List<T> toList() {
            //noinspection unchecked
            return (List<T>) data.clone();
        }

        public void clear() {
            data.clear();
        }
    }

    public static Locale getLocaleFromString(String languageCode) {
        String[] parts = languageCode.split("_", -1);
        if (parts.length == 1) return new Locale(parts[0]);
        else if (parts.length == 2
                 || (parts.length == 3 && parts[2].startsWith("#")))
            return new Locale(parts[0], parts[1]);
        else return new Locale(parts[0], parts[1], parts[2]);
    }
}
