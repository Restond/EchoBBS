(function () {
    const originalFetch = window.fetch;

    window.fetch = function (input, init) {
        init = init || {};

        const method = (init.method || 'GET').toUpperCase();
        const safeMethods = ['GET', 'HEAD', 'OPTIONS', 'TRACE'];

        if (!safeMethods.includes(method)) {
            const tokenMeta = document.querySelector('meta[name="_csrf"]');
            const headerMeta = document.querySelector('meta[name="_csrf_header"]');

            if (tokenMeta && headerMeta) {
                const token = tokenMeta.getAttribute('content');
                const headerName = headerMeta.getAttribute('content');

                init.headers = new Headers(init.headers || {});
                init.headers.set(headerName, token);
            }
        }
        return originalFetch(input, init);
    };
})();