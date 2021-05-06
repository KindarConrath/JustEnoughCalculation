package me.towdium.jecalculation.gui.widgets;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import me.towdium.jecalculation.data.label.ILabel;
import me.towdium.jecalculation.polyfill.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Author: towdium
 * Date:   17-8-17.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SideOnly(Side.CLIENT)
public class WLabelGroup extends WContainer {
    ArrayList<WLabel> labels = new ArrayList<>();
    ListenerValue<? super WLabelGroup, Integer> lsnrUpdate;
    ListenerValue<? super WLabelGroup, Integer> lsnrClick;

    public WLabelGroup(int xPos, int yPos, int column, int row, boolean multiple, boolean accept) {
        this(xPos, yPos, column, row, 18, 18, multiple, accept);
    }

    public WLabelGroup(int xPos,
                       int yPos,
                       int column,
                       int row,
                       int xSize,
                       int ySize,
                       boolean multiple,
                       boolean accept) {
        for (int j = 0; j < row; j++) {
            int r = j;
            IntStream.range(0, column).forEach(c -> {
                WLabel l = new WLabel(xPos + c * xSize, yPos + r * ySize, xSize, ySize, multiple, accept).setLsnrUpdate(
                        (i, v) -> {
                            if (lsnrUpdate != null)
                                lsnrUpdate.invoke(this, r * column + c);
                        }).setLsnrClick(i -> {
                    if (lsnrClick != null)
                        lsnrClick.invoke(this, r * column + c);
                });
                labels.add(l);
                add(l);
            });
        }
    }

    public WLabel get(int index) {
        return labels.get(index);
    }

    public List<ILabel> getLabels() {
        return labels.stream().map(WLabel::getLabel).collect(Collectors.toList());
    }

    public void setLabel(ILabel label, int index) {
        labels.get(index).setLabel(label);
    }

    public void setLabel(List<ILabel> labels, int start) {
        for (WLabel label : this.labels)
            label.setLabel(start < labels.size() ? labels.get(start++) : ILabel.EMPTY);
    }

    public WLabelGroup setLsnrUpdate(ListenerValue<? super WLabelGroup, Integer> listener) {
        lsnrUpdate = listener;
        return this;
    }

    public WLabelGroup setLsnrClick(ListenerValue<? super WLabelGroup, Integer> listener) {
        lsnrClick = listener;
        return this;
    }

    public WLabelGroup setFormatter(Function<ILabel, String> f) {
        labels.forEach(i -> i.setFormatter(f));
        return this;
    }
}