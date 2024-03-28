package org.vaadin.tatu.vaadincreate.uiunittest.testers;

import org.vaadin.tatu.vaadincreate.uiunittest.Tester;

import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;

public class TabSheetTester extends Tester<TabSheet> {

    public TabSheetTester(TabSheet tabSheet) {
        super(tabSheet);
    }

    public Tab tab(String caption) {
        for (int i = 0; i < getComponent().getComponentCount(); i++) {
            Tab tab = getComponent().getTab(i);
            if (tab.getCaption().contains(caption)) {
                return tab;
            }
        }
        return null;
    }

    public void click(Tab tab) {
        int index = 0;
        for (int i = 0; i < getComponent().getComponentCount(); i++) {
            Tab t = getComponent().getTab(i);
            if (t.equals(tab)) {
                index = i;
                break;
            }
        }
        click(index);
    }

    public void click(int index) {
        var iter = getComponent().iterator();
        Component comp = null;
        int i = 0;
        while (iter.hasNext()) {
            var c = iter.next();
            if (i == index) {
                comp = c;
                break;
            }
            i++;
        }
        getComponent().setSelectedTab(comp, true);
    }

    public Component current() {
        return getComponent().getSelectedTab();
    }

    @Override
    protected TabSheet getComponent() {
        return super.getComponent();
    }
}
