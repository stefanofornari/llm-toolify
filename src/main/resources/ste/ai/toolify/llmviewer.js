const md = window.markdownit({
    highlight: function (str, lang) {
        if (lang && hljs.getLanguage(lang)) {
            try {
                return '<pre><code class="hljs language-' + lang + '">' +
                       hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
                       '</code></pre>';
            } catch (__) {}
        }

        return '<pre><code class="hljs">' + md.utils.escapeHtml(str) + '</code></pre>';
    }
});


function content(text) {
    if (!text) {
        return;
    }

    const mainDiv = document.getElementById('content');
    const contentDiv = document.createElement('div');

    contentDiv.innerHTML = md.render(text.trim());

    mainDiv.appendChild(contentDiv);
}