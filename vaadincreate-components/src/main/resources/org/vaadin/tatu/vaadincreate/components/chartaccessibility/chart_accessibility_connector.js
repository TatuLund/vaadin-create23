window.org_vaadin_tatu_vaadincreate_components_ChartAccessibilityExtension = function () {
  "use strict";

  let connector = this;
  let state = connector.getState();

  /**
   * Finds the chart container based on the chart element
   *
   * @param {HTMLElement} element - The chart element
   * @returns {Object|null} - The chart container or null if not found
   */
  function _findChartContainer(element) {
    // Navigate to find the actual chart container
    // Chart structure: div[id] -> div.wrapper -> svg
    let wrapperDiv = element.querySelector("div");
    if (!wrapperDiv) {
      return null;
    }

    let svg = wrapperDiv.querySelector("svg");
    if (!svg) {
      return null;
    }

    return {
      chartElement: element,
      wrapperDiv: wrapperDiv,
      svg: svg,
    };
  }

  /**
   * Applies accessibility patches to a single chart
   *
   * @param {HTMLElement} element - The chart element
   * @param {boolean} legendsClickable - Whether the legend items are clickable
   * @returns {boolean} - True if the chart was successfully patched, false otherwise
   */
  function _patchSingleChart(element, legendsClickable) {
    let chart = _findChartContainer(element);
    if (!chart) {
      return;
    }

    let svg = chart.svg;
    let chartElement = chart.chartElement;

    try {
      // Remove Highcharts desc-banners (announced by screen readers)
      let desc = svg.querySelector("desc");
      desc?.parentNode?.removeChild(desc);

      // Add keyboard accessibility to legend items within this chart
      // Try multiple scopes: chartElement, svg, and document (for debugging)
      let legendItems = svg.querySelectorAll(".highcharts-legend-item");

      legendItems.forEach((legendButton) => {
        if (!legendButton.hasAttribute("data-accessibility-patched")) {
          legendButton.setAttribute("tabindex", "0");
          legendButton.setAttribute("role", "button");
          legendButton.setAttribute(
            "aria-label",
            legendsClickable + " " + legendButton.textContent
          );
          legendButton.toggleAttribute("data-accessibility-patched");

          legendButton.addEventListener("keyup", (event) => {
            if (event.key === "Enter" || event.key === " ") {
              event.preventDefault();
              legendButton.dispatchEvent(new CustomEvent("click"));
            }
          });
        }
      });

      // Setup context menu accessibility for buttons within this chart
      let buttons = svg.querySelectorAll(".highcharts-button");

      let buttonIndex = 0;

      buttons.forEach((button) => {
        if (!button.hasAttribute("data-accessibility-patched")) {
          buttonIndex++;
          let menuId = "context-menu-" + (element.id || "") + "-" + buttonIndex;

          button.setAttribute("tabindex", "0");
          button.setAttribute("role", "button");
          button.setAttribute("aria-haspopup", "true");
          button.setAttribute("aria-controls", menuId);
          button.setAttribute("aria-label", state.contextMenu);
          button.getElementsByTagName("title")[0].textContent =
            state.contextMenu;
          button.toggleAttribute("data-accessibility-patched");

          button.addEventListener("keyup", (event) => {
            if (
              event.key === "Enter" ||
              event.key === " " ||
              event.key === "ArrowDown"
            ) {
              event.preventDefault();
              button.dispatchEvent(new CustomEvent("click"));
            }
          });

          button.addEventListener("click", () => {
            _setupContextMenu(button, menuId, chartElement);
            button.setAttribute("aria-expanded", "true");
          });
        }
      });
    } catch (error) {
      console.error(
        "ChartAccessibilityExtension: Error patching chart:",
        element.id || "",
        error
      );
    }
  }

  /**
   * Closes the context menu
   *
   * @param {HTMLElement} menu
   * @param {HTMLElement} button
   */
  function _closeMenu(menu, button) {
    menu.style.display = "none";
    button.focus();
    button.removeAttribute("aria-expanded");
    let rect = button.getElementsByTagName("rect")[0];
    if (rect) {
      rect.setAttribute("fill", "white");
      rect.setAttribute("stroke", "none");
    }
  }

  /**
   * Sets up context menu accessibility
   *
   * @param {HTMLElement} button
   * @param {string} menuId
   * @param {HTMLElement} chartElement
   */
  function _setupContextMenu(button, menuId, chartElement) {
    // Wait for context menu to appear
    let attempts = 0;
    let maxAttempts = 10;

    function _waitForMenu() {
      attempts++;

      let menu = chartElement.querySelector(".highcharts-contextmenu");
      if (menu && menu.style.display !== "none") {
        let menuItems = menu.children[0];
        if (
          menuItems &&
          !menuItems.hasAttribute("data-accessibility-patched")
        ) {
          menuItems.setAttribute("tabindex", "-1");
          menuItems.setAttribute("role", "menu");
          menuItems.setAttribute("aria-label", "Chart context menu");
          menuItems.setAttribute("id", menuId);
          menuItems.toggleAttribute("data-accessibility-patched");

          let items = menuItems.querySelectorAll("div");
          let itemIndex = 0;
          items.forEach((item) => {
            item.innerText = state.menuEntries[itemIndex] || item.innerText;
            itemIndex++;
            item.setAttribute("tabindex", "0");
            item.setAttribute("role", "menuitem");

            item.addEventListener("keyup", (event) => {
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                item.dispatchEvent(new CustomEvent("click"));
              }
              if (event.key === "Escape") {
                _closeMenu(menu, button);
              }
            });
          });

          menuItems.addEventListener("focusout", (event) => {
            if (
              event.relatedTarget &&
              !menuItems.contains(event.relatedTarget)
            ) {
              _closeMenu(menu, button);
            }
          });

          // Focus first menu item
          if (items.length > 0) {
            items[0].focus();
          }
        } else if (menuItems?.hasAttribute("data-accessibility-patched")) {
          let items = menuItems.querySelectorAll("div");
          if (items.length > 0) {
            items[0].focus();
          }
        }
      } else if (attempts < maxAttempts) {
        setTimeout(_waitForMenu, 50);
      }
    }

    setTimeout(_waitForMenu, 50);
  }

  /**
   * Applies patches with retry logic
   *
   * @param {HTMLElement} element Chart element to patch
   */
  function _applyPatchesWithRetry(element) {
    let legendsClickable = state.legendsClickable || "Click to toggle";
    let maxAttempts = state.maxAttempts || 20;
    let retryInterval = state.retryInterval || 100;

    let attempts = 0;

    function _tryPatch() {
      attempts++;
      _patchSingleChart(element, legendsClickable);

      if (attempts < maxAttempts) {
        setTimeout(_tryPatch, retryInterval);
      }
    }

    _tryPatch();
  }

  /**
   * Public function to reapply patches
   */
  connector.applyPatches = () => {
    _applyPatchesWithRetry(this.chartElement);
  };

  /**
   * Called when state changes
   */
  connector.onStateChange = () => {
    this.chartElement = this.getElement(this.getParentId());
  };
};
