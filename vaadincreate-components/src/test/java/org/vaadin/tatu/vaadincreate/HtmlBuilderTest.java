package org.vaadin.tatu.vaadincreate;

import static org.junit.Assert.*;

import org.junit.Test;
import org.vaadin.tatu.vaadincreate.AttributeExtension.AriaAttributes;
import org.vaadin.tatu.vaadincreate.AttributeExtension.AriaRoles;

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
