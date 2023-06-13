var UA = (function (window, navigator) {
    "use strict";

    var ua = (window.navigator && navigator.userAgent) || "";

    function detect(pattern) {
        return function () {
            return (pattern).test(ua);
        };
    }

    return {
        isChrome: detect(/webkit\W.*(chrome|chromium)\W/i),
        isFirefox: detect(/mozilla.*\Wfirefox\W/i),
        isGecko: detect(/mozilla(?!.*webkit).*\Wgecko\W/i),
        isIE: function () {
            return navigator.appName === "Microsoft Internet Explorer" || detect(/\bTrident\b/);
        },
        isKindle: detect(/\W(kindle|silk)\W/i),
        isMobile: detect(/(iphone|ipod|(android.*?mobile)|blackberry|nokia)/i),
        isOpera: detect(/opera.*\Wpresto\W/i),
        isSafari: detect(/webkit\W(?!.*chrome).*safari\W/i),
        isTablet: detect(/(ipad|android(?!.*mobile))/i),
        isTV: detect(/googletv|sonydtv/i),
        isWebKit: detect(/webkit\W/i),
        isAndroid: detect(/android/i),
        isIOS: detect(/(ipad|iphone|ipod)/i),
        isIPad: detect(/ipad/i),
        isIPhone: detect(/iphone/i),
        isIPod: detect(/ipod/i),
        whoami: function () {
            return ua;
        }
    };
}(window, navigator));
