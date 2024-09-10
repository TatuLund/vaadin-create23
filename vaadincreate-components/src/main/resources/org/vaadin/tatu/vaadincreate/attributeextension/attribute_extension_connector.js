window.org_vaadin_tatu_vaadincreate_AttributeExtension = function () {
  this.onStateChange = function () {
    const element = this.getElement(this.getParentId());
    if (element) {
      const attributes = this.getState().attributes;
      for (let attr in attributes) {
        if (attributes.hasOwnProperty(attr)) {
          try {
            element.setAttribute(attr, attributes[attr]);
          } catch (e) {
            // IE8 does not support type='number' - just ignore it
          }
        }
      }
    }
  };
};
