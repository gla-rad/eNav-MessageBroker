/**
 * The key under which the selected colour theme is remembered.
 */
const THEME_STORAGE_KEY = 'msg-broker-theme';

/**
 * Returns the colour theme currently applied to the document.
 *
 * @return {String} Either "light" or "dark"
 */
function getTheme() {
    return document.documentElement.getAttribute('data-bs-theme') === 'dark' ? 'dark' : 'light';
}

/**
 * Applies the provided colour theme to the document and remembers the choice
 * for the next visit.
 *
 * @param {String}  theme   The theme to apply ("light" or "dark")
 */
function setTheme(theme) {
    const applied = theme === 'dark' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-bs-theme', applied);
    try {
        window.localStorage.setItem(THEME_STORAGE_KEY, applied);
    } catch (e) {
        // Storage may be unavailable (private mode), the theme still applies
    }
}

/**
 * Pops up a short lived notification in the bottom right corner of the screen.
 * This is used for the operations that succeed quietly and therefore do not
 * deserve a modal dialog of their own.
 *
 * @param {String}  message     The message to be displayed
 * @param {String}  variant     One of "info", "success", "warning" or "danger"
 */
function showToast(message, variant) {
    const icons = {
        info: 'fa-circle-info text-primary',
        success: 'fa-circle-check text-success',
        warning: 'fa-triangle-exclamation text-warning',
        danger: 'fa-circle-exclamation text-danger'
    };
    const icon = icons[variant] || icons.info;

    // Make sure the stack that hosts the toasts exists
    let stack = $('#toastStack');
    if (stack.length === 0) {
        stack = $('<div id="toastStack" class="toast-stack"></div>').appendTo('body');
    }

    // Build, show and then dispose of the toast
    const toast = $(`
        <div class="toast align-items-center" role="alert" aria-live="polite" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body d-flex align-items-center gap-2">
                    <i class="fa-solid ${icon}"></i><span></span>
                </div>
                <button type="button" class="btn-close me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>`);
    toast.find('.toast-body span').text(message);
    stack.append(toast);
    toast.on('hidden.bs.toast', () => toast.remove());
    new bootstrap.Toast(toast[0], { delay: 4000 }).show();
}

/**
 * A helper function to prettify the XML string provided.
 *
 * @param {String}  xml     The XML input to be prettified
 */
function formatXml(xml) {
    var formatted = '';
    var reg = new RegExp("(>)(<)(\/*)", "g");
    xml = xml != undefined ? xml.replace(reg, '$1\r\n$2$3') : '';
    var pad = 0;
    var xmlArray = xml.split('\r\n');
    jQuery.each(xmlArray, (index, node) => {
        var last = index === xmlArray.length - 1;
        var indent = 0;
        if (node.match( /.+<\/\w[^>]*>$/ )) {
            indent = 0;
        } else if (node.match( /^<\/\w/ )) {
            if (pad != 0) {
                pad -= 1;
            }
        } else if (node.match( /^<\w[^>]*[^\/]>.*$/ )) {
            indent = 1;
        } else {
            indent = 0;
        }

        var padding = '';
        for (var i = 0; i < pad; i++) {
            padding += '  ';
        }

        formatted += padding + node + (last ? '' : '\r\n');
        pad += indent;
    });

    return formatted;
}

/**
 * Escapes a value so that it can safely be dropped into an HTML string. Any
 * empty value is rendered as an em dash to keep the tables tidy.
 *
 * @param {*}   value   The value to be escaped
 * @return {String} The HTML-safe representation of the value
 */
function escapeHtml(value) {
    if (value === null || value === undefined || value === '') {
        return '&mdash;';
    }
    return $('<div>').text(value).html();
}

/**
 * Renders an identifier - a UUID or an ID code - in a monospaced font so that
 * long opaque strings stay scannable.
 *
 * @param {String}  value   The identifier
 * @return {String} The identifier markup
 */
function renderIdentifier(value) {
    if (!value) {
        return '<span class="cell-muted">&mdash;</span>';
    }
    return `<span class="cell-mono">${escapeHtml(value)}</span>`;
}

/**
 * Renders a date-time in a compact and sortable representation.
 *
 * @param {Date}    date    The date-time to be rendered
 * @return {String} The date markup
 */
function renderDateTime(date) {
    const pad = (number) => String(number).padStart(2, '0');
    const shown = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
        + ` ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
    return `<span class="cell-mono">${escapeHtml(shown)}</span>`;
}

/**
 * Wires up the elements that are present on every page - currently just the
 * colour theme toggle sitting in the navigation bar.
 */
$(() => {
    $('[data-theme-toggle]').on('click', () => setTheme(getTheme() === 'dark' ? 'light' : 'dark'));
});
