window.org_vaadin_tatu_vaadincreate_components_AttributeExtension = function () {
  this.onStateChange = () => {
    const element = this.getElement(this.getParentId());
    if (!element) return;

    const { attributes, removals } = this.getState();

    Object.keys(attributes).forEach(attr => {
      try {
        element.setAttribute(attr, attributes[attr]);
      } catch (error) {
        console.error(`Failed to set attribute ${attr} to ${attributes[attr]}`, error);
      }
    });

    removals.forEach(attr => element.removeAttribute(attr));
  };
};