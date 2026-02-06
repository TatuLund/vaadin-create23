package org.vaadin.tatu.vaadincreate.components;

import static org.junit.Assert.*;

import org.junit.Test;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.components.AttributeExtension.AriaRoles;

@SuppressWarnings("null")
public class HtmlBuilderTest {

    @Test
    public void spanWithAttributesAndText() {
        var html = Html.span().id("id1").cls("c1", "c2").text("Hello & <World>")
                .build();
        assertEquals(
                "<span id=\"id1\" class=\"c1 c2\">Hello &amp; &lt;World&gt;</span>",
                html);
    }

    @Test
    public void spanSelfClosingWhenNoContent() {
        var html = Html.span().cls("empty").build();
        assertEquals("<span class=\"empty\"></span>", html);
    }

    @Test
    public void ariaAndRoleAttributes() {
        var html = Html.span().attr(AriaAttributes.LABEL, "Label")
                .role(AriaRoles.BUTTON).tabindex(1).build();
        assertTrue(html.contains("aria-label=\"Label\""));
        assertTrue(html.contains("role=\"button\""));
        assertTrue(html.contains("tabindex=\"1\""));
    }

    @Test
    public void multipleClassesJoined() {
        var html = Html.div().cls("class1", "class2", "class3").build();
        assertEquals("<div class=\"class1 class2 class3\"></div>", html);
    }

    @Test
    public void allAriaAttributes() {
        for (var field : AriaAttributes.class.getDeclaredFields()) {
            try {
                var attrName = (String) field.get(null);
                var html = Html.div().attr(attrName, "value").build();
                assertEquals("<div " + attrName + "=\"value\"></div>", html);
            } catch (IllegalAccessException e) {
                fail("Failed to access field: " + e.getMessage());
            }
        }
    }

    @Test
    public void h1WithText() {
        var html = Html.h1().text("Title").build();
        assertEquals("<h1>Title</h1>", html);
    }

    @Test
    public void sanitationOfRawHtml() {
        var html = Html.div().raw("<script>alert('x')</script>").build();
        assertEquals("<div></div>", html);
        html = Html.div().raw("<span>Safe</span>").build();
        assertEquals("<div><span>Safe</span></div>", html);
        html = Html.div().raw("<span onclick='evil()'>X</span>").build();
        assertEquals("<div><span>X</span></div>", html);
    }

    @Test
    public void nestedStructureEscapesChildren() {
        var outer = Html.div().add(Html.span().text("<unsafe>"));
        var html = outer.build();
        assertEquals("<div><span>&lt;unsafe&gt;</span></div>", html);
    }

    @Test
    public void rawHtmlInsertedUnescaped() {
        var html = Html.div().raw("<span class='x'>X</span>").build();
        assertEquals("<div><span class=\"x\">X</span></div>", html);
    }

    @Test
    public void boldTagRendersAndEscapes() {
        var html = Html.b().text("<bold>").build();
        assertEquals("<b>&lt;bold&gt;</b>", html);
    }

    @Test
    public void brTagSelfClosing() {
        var html = Html.br().build();
        assertEquals("<br/>", html);
    }
}
