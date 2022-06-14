package me.towdium.jecalculation.data.structure;


import net.minecraft.nbt.CompoundTag;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;

/**
 * Author: Towdium
 * Date: 19-1-20
 */
public class RecordMath implements IRecord {
    public static MathContext context = new MathContext(7, RoundingMode.HALF_UP);
    public static final int DOT_NONE = -1;
    static final String KEY_CURRENT = "current";
    static final String KEY_LAST = "last";
    static final String KEY_OPERATOR = "operator";
    static final String KEY_STATE = "state";

    public BigDecimal last;
    public State state;
    public Operator operator;
    String current;

    public enum State {INPUT, OUTPUT, ERROR}

    public enum Operator {
        EQUALS, PLUS, MINUS, TIMES, DIVIDE;

        public BigDecimal operate(BigDecimal a, BigDecimal b) {
            return switch (this) {
                case PLUS -> a.add(b);
                case MINUS -> a.subtract(b);
                case TIMES -> a.multiply(b);
                case DIVIDE -> a.divide(b, context);
                case EQUALS -> b;
                default -> throw new RuntimeException("Internal Error");
            };
        }
    }

    public RecordMath(State state, Operator operator, BigDecimal last,
                      boolean sign, int dot, List<BigDecimal> numbers) {
        this.state = state;
        this.operator = operator;
        this.last = last;
        StringBuilder sb = new StringBuilder();
        if (!sign) sb.append('-');
        numbers.forEach(i -> sb.append(i.toString()));
        if (dot != DOT_NONE) sb.insert(sb.length() - dot, '.');
        current = sb.toString();
    }

    public RecordMath(CompoundTag nbt) {
        state = State.values()[nbt.getInt(KEY_STATE)];
        operator = Operator.values()[nbt.getInt(KEY_OPERATOR)];
        last = nbt.contains(KEY_LAST) ? new BigDecimal(nbt.getString(KEY_LAST)) : BigDecimal.ZERO;
        current = nbt.getString(KEY_CURRENT);
    }

    public CompoundTag serialize() {
        CompoundTag ret = new CompoundTag();
        ret.putString(KEY_CURRENT, current);
        ret.putString(KEY_LAST, last.toString());
        ret.putInt(KEY_OPERATOR, operator.ordinal());
        ret.putInt(KEY_STATE, state.ordinal());
        return ret;
    }

    public boolean getSign() {
        return current == null || current.isEmpty() || current.charAt(0) != '-';
    }

    public LinkedList<BigDecimal> getNumbers() {
        LinkedList<BigDecimal> ret = new LinkedList<>();
        if (current == null) return ret;
        for (int i = 0; i < current.length(); i++) {
            char ch = current.charAt(i);
            if (ch != '-' && ch != '.') ret.add(new BigDecimal(ch - '0'));
        }
        return ret;
    }

    public int getDot() {
        if (current == null) return DOT_NONE;
        int index = current.indexOf('.');
        return index == -1 ? DOT_NONE : current.length() - index - 1;
    }
}
