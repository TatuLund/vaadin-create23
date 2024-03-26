package org.vaadin.tatu.vaadincreate.uiunittest;

import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;

public class TabSheetTester extends Tester<TabSheet> {

    private TabSheet tabSheet;

    public TabSheetTester(TabSheet tabSheet) {
        super(tabSheet);
        this.tabSheet = tabSheet;
    }

    public Tab tab(String caption) {
        for (int i = 0; i < tabSheet.getComponentCount(); i++) {
            Tab tab = tabSheet.getTab(i);
            if (tab.getCaption().contains(caption)) {
                return tab;
            }
        }
        return null;
    }

    public void click(Tab tab) {
        int index = 0;
        for (int i = 0; i < tabSheet.getComponentCount(); i++) {
            Tab t = tabSheet.getTab(i);
            if (t.equals(tab)) {
                index = i;
                break;
            }
        }
        click(index);
    }

    public void click(int index) {
        var iter = tabSheet.iterator();
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
        tabSheet.setSelectedTab(comp, true);
    }

    public Component current() {
        return tabSheet.getSelectedTab();
    }
}
