window.org_vaadin_tatu_vaadincreate_ChartAccessibilityExtension = function () {
  "use strict";

  var connector = this;
  var state = connector.getState();

  // Store the current patch state to avoid duplicate patches
  var patchedElements = new Set();

  /**
   * Finds the chart container based on the chart ID
   *
   * @param {string} chartId - The ID of the chart
   * @returns {Object|null} - The chart container or null if not found
   */
  function _findChartContainer(chartId) {
    if (!chartId) {
      return null;
    }

    var chartElement = document.getElementById(chartId);
    if (!chartElement) {
      console.warn(
        "ChartAccessibilityExtension: Chart element not found with ID:",
        chartId
      );
      return null;
    }

    // Navigate to find the actual chart container
    // Chart structure: div[id] -> div.wrapper -> svg
    var wrapperDiv = chartElement.querySelector("div");
    if (!wrapperDiv) {
      return null;
    }

    var svg = wrapperDiv.querySelector("svg");
    if (!svg) {
      return null;
    }

    console.debug(
      "ChartAccessibilityExtension: Found chart structure for",
      chartId,
      ":",
      {
        chartElement: chartElement.tagName,
        wrapperDiv: wrapperDiv.tagName,
        svg: svg.tagName,
        chartElementClasses: chartElement.className,
        wrapperDivClasses: wrapperDiv.className,
      }
    );

    return {
      chartElement: chartElement,
      wrapperDiv: wrapperDiv,
      svg: svg,
    };
  }

  /**
   * Applies accessibility patches to a single chart
   *
   * @param {string} chartId - The ID of the chart
   * @param {boolean} legendsClickable - Whether the legend items are clickable
   * @returns {boolean} - True if the chart was successfully patched, false otherwise
   */
  function _patchSingleChart(chartId, legendsClickable) {
    var chart = _findChartContainer(chartId);
    if (!chart) {
      return false;
    }

    var svg = chart.svg;
    var chartElement = chart.chartElement;

    // Check if this chart has already been patched
    if (patchedElements.has(chartId)) {
      console.debug(
        "ChartAccessibilityExtension: Chart already patched:",
        chartId
      );
      return true;
    }

    try {
      // Remove Highcharts desc-banners (announced by screen readers)
      var desc = svg.querySelector("desc");
      if (desc && desc.parentNode) {
        desc.parentNode.removeChild(desc);
      }

      // Add keyboard accessibility to legend items within this chart
      // Try multiple scopes: chartElement, svg, and document (for debugging)
      var legendItems = svg.querySelectorAll(".highcharts-legend-item");

      console.debug(
        "ChartAccessibilityExtension: Found",
        legendItems.length,
        "legend items for chart:",
        chartId
      );

      legendItems.forEach((legendButton) => {
        if (!legendButton.hasAttribute("data-accessibility-patched")) {
          legendButton.setAttribute("tabindex", "0");
          legendButton.setAttribute("role", "button");
          legendButton.setAttribute(
            "aria-label",
            legendsClickable + " " + legendButton.textContent
          );
          legendButton.toggleAttribute("data-accessibility-patched");
          console.debug(
            "ChartAccessibilityExtension: Patched legend item:",
            legendButton.textContent,
            "for chart:",
            chartId
          );

          legendButton.addEventListener("keyup", (event) => {
            if (event.key === "Enter" || event.key === " ") {
              event.preventDefault();
              legendButton.dispatchEvent(new CustomEvent("click"));
            }
          });
        }
      });

      // Setup context menu accessibility for buttons within this chart
      var buttons = svg.querySelectorAll(".highcharts-button");

      console.debug(
        "ChartAccessibilityExtension: Found",
        buttons.length,
        "buttons for chart:",
        chartId
      );

      var buttonIndex = 0;

      buttons.forEach(function (button) {
        if (!button.hasAttribute("data-accessibility-patched")) {
          buttonIndex++;
          var menuId = "context-menu-" + chartId + "-" + buttonIndex;

          button.setAttribute("tabindex", "0");
          button.setAttribute("role", "button");
          button.setAttribute("aria-haspopup", "true");
          button.setAttribute("aria-controls", menuId);
          button.setAttribute("aria-label", state.contextMenu);
          button.getElementsByTagName("title")[0].textContent = state.contextMenu;
          button.toggleAttribute("data-accessibility-patched");
          console.debug(
            "ChartAccessibilityExtension: Patched button",
            buttonIndex,
            "for chart:",
            chartId
          );

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

          button.addEventListener("click", (event) => {
            _setupContextMenu(button, menuId, chartElement);
            button.setAttribute("aria-expanded", "true");
          });
        }
      });

      patchedElements.add(chartId);
      console.log(
        "ChartAccessibilityExtension: Successfully patched chart:",
        chartId
      );
      return true;
    } catch (error) {
      console.error(
        "ChartAccessibilityExtension: Error patching chart:",
        chartId,
        error
      );
      return false;
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
    var rect = button.getElementsByTagName("rect")[0];
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
    var attempts = 0;
    var maxAttempts = 10;

    function _waitForMenu() {
      attempts++;

      var menu = chartElement.querySelector(".highcharts-contextmenu");
      if (menu && menu.style.display !== "none") {
        var menuItems = menu.children[0];
        if (
          menuItems &&
          !menuItems.hasAttribute("data-accessibility-patched")
        ) {
          menuItems.setAttribute("tabindex", "-1");
          menuItems.setAttribute("role", "menu");
          menuItems.setAttribute("aria-label", "Chart context menu");
          menuItems.setAttribute("id", menuId);
          menuItems.toggleAttribute("data-accessibility-patched");

          var items = menuItems.querySelectorAll("div");
          var itemIndex = 0;
          items.forEach(function (item) {
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
        } else if (
          menuItems &&
          menuItems.hasAttribute("data-accessibility-patched")
        ) {
          var items = menuItems.querySelectorAll("div");
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
   */
  function _applyPatchesWithRetry() {
    var chartId = state.chartId;
    var legendsClickable = state.legendsClickable || "Click to toggle";
    var maxAttempts = state.maxAttempts || 20;
    var retryInterval = state.retryInterval || 100;

    if (!chartId) {
      console.warn("ChartAccessibilityExtension: No chart ID specified");
      return;
    }

    console.debug(
      "ChartAccessibilityExtension: Starting patch attempts for chart:",
      chartId
    );

    var attempts = 0;

    function _tryPatch() {
      attempts++;
      console.debug(
        "ChartAccessibilityExtension: Attempt",
        attempts,
        "for chart:",
        chartId
      );

      if (_patchSingleChart(chartId, legendsClickable)) {
        console.debug(
          "ChartAccessibilityExtension: Patches applied after",
          attempts,
          "attempts for chart:",
          chartId
        );
        // Notify server that patches were applied
        connector.getState().patchesApplied = true;
        return;
      }

      if (attempts < maxAttempts) {
        setTimeout(_tryPatch, retryInterval);
      } else {
        console.warn(
          "ChartAccessibilityExtension: Failed to apply patches after",
          maxAttempts,
          "attempts for chart:",
          chartId
        );
      }
    }

    _tryPatch();
  }

  /**
   * Public function to reapply patches
   */
  connector.applyPatches = () => {
    if (state.chartId) {
      patchedElements.delete(state.chartId);
      _applyPatchesWithRetry();
    }
  };

  /**
   * Called when state changes
   */
  connector.onStateChange = () => {
    // NOP
  };
};
