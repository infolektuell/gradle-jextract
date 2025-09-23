/** @type {import('prettier').Options} */
export default {
    plugins: ['prettier-plugin-astro'],
    overrides: [
        {
            files: '*.astro',
            options: {
                parser: 'astro',
            },
        },
        {
            files: '*.svelte',
            options: {
                parser: 'svelte',
            },
        },
    ],
    singleQuote: true,
    semi: false,
    astroAllowShorthand: true,
}
