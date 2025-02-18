window.org_vaadin_tatu_vaadincreate_AttributeExtension = function () {
  this.onStateChange = function () {
    const element = this.getElement(this.getParentId());
    if (element) {
      const attributes = this.getState().attributes;
      for (var attr in attributes) {
        if (attributes.hasOwnProperty(attr)) {
          try {
            element.setAttribute(attr, attributes[attr]);
          } catch (e) {
            console.error('Failed to set attribute ' + attr + ' to ' + attributes[attr], e);
          }
        }
      }
      this.getState().removals.forEach(function(attr) {
        element.removeAttribute(attr);
      });
    }
  };
};
