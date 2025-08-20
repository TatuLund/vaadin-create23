package org.vaadin.tatu.vaadincreate.stats;

import com.vaadin.ui.JavaScript;

public class ChartAccessibility {
    
    private ChartAccessibility() {
        // Private constructor to prevent instantiation
    }

    public static void patchCharts(String legendsClickable) {
        // Remove Highcharts desc-banners, they are announced by NVDA
        // And add keyboard accessibility to legend items and buttons
        JavaScript
                .eval("""
                        const patchCharts = () => {
                            Array.from(document.getElementsByTagName('svg'))
                                .forEach(el => {
                                    const desc = el.getElementsByTagName('desc')[0];
                                    el.removeChild(desc);
                                });
                            Array.from(document.getElementsByClassName('highcharts-legend-item'))
                                .forEach(el => {
                                    el.setAttribute('tabindex', '0');
                                    el.setAttribute('role', 'button');
                                    el.setAttribute('aria-label', '%s ' + el.textContent);
                                    el.addEventListener('keyup', (e) => {
                                        if (e.key === 'Enter' || e.key === ' ') {
                                            el.dispatchEvent(new CustomEvent('click'));
                                        }
                                    });
                                });
                            const setupMenu = (button, index) => setTimeout(() => {
                                Array.from(document.getElementsByClassName('highcharts-contextmenu'))
                                    .forEach(el => {
                                        const menuItems = el.children[0];
                                        menuItems.setAttribute('tabindex', '-1');
                                        menuItems.setAttribute('role', 'menu');
                                        menuItems.setAttribute('aria-label', 'Context menu');
                                        menuItems.setAttribute('id', 'context-menu-' + index);
                                        Array.from(menuItems.getElementsByTagName('div'))
                                            .forEach(item => {
                                                item.setAttribute('tabindex', '0');
                                                item.setAttribute('role', 'menuitem');
                                                item.addEventListener('keyup', (e) => {
                                                    if (e.key === 'Enter' || e.key === ' ') {
                                                    item.dispatchEvent(new CustomEvent('click'));
                                                }
                                                if (e.key === 'Escape') {
                                                    el.style.display = 'none';
                                                    button.focus();
                                                    button.removeAttribute('aria-expanded');
                                                }
                                            });
                                        });
                                        menuItems.addEventListener('focusout', (e) => {
                                            if (e.relatedTarget && !menuItems.contains(e.relatedTarget)) {
                                                el.style.display = 'none';
                                                button.focus();
                                                button.removeAttribute('aria-expanded');
                                            }
                                        });
                                        menuItems.children[0].focus();                                   
                                    });
                            }, 100);

                            let index = 0;
                            Array.from(document.getElementsByClassName('highcharts-button'))
                                .forEach(button => {
                                    index++;
                                    button.setAttribute('tabindex', '0');
                                    button.setAttribute('role', 'button');
                                    button.setAttribute('aria-haspopup', 'true');
                                    button.setAttribute('aria-controls', 'context-menu-' + index);
                                    button.addEventListener('keyup', (e) => {
                                        if (e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowDown') {
                                            button.dispatchEvent(new CustomEvent('click'));
                                        }
                                    });
                                    button.addEventListener('click', (e) => {
                                        setupMenu(button, index);
                                        button.setAttribute('aria-expanded','true');
                                    });
                                });
                        };

                        function patchChartsWithRetry(maxAttempts = 20, interval = 100) {
                            let attempts = 0;
            
                            const tryPatch = () => {
                                attempts++;
                
                                const svgs = document.getElementsByTagName('svg');
                                const legendItems = document.getElementsByClassName('highcharts-legend-item');
                                const buttons = document.getElementsByClassName('highcharts-button');
                
                                // Check if charts are ready
                                const chartsReady = svgs.length == 3 && 
                                    legendItems.length == 2 && buttons.length == 3;

                                if (chartsReady) {
                                    patchCharts();
                                    console.log('Chart accessibility patches applied after', attempts, 'attempts');
                                    return true;
                                } else if (attempts < maxAttempts) {
                                    setTimeout(tryPatch, interval);
                                    return false;
                                } else {
                                    console.warn('Chart accessibility patching failed after', maxAttempts, 'attempts');
                                    return false;
                                }
                            };
            
                            tryPatch();
                        }

                        patchChartsWithRetry();
                        """
                        .formatted(legendsClickable));

    }
}
