// @ts-check
import { defineConfig } from 'astro/config'
import starlight from '@astrojs/starlight'
import starlightAutoImport from './src/plugins/starlight-auto-import'

// https://astro.build/config
export default defineConfig({
    site: 'https://infolektuell.github.io',
    base: '/gradle-jextract/',
    trailingSlash: 'always',
    integrations: [
        starlight({
            plugins: [starlightAutoImport()],
            title: 'Gradle Jextract Plugin',
            description: 'Generates Java bindings from native library headers using Jextract',
            logo: {
                src: './src/assets/logo.svg',
            },
            social: [{ icon: 'github', label: 'GitHub', href: 'https://github.com/infolektuell/gradle-jextract' }],
            editLink: {
                baseUrl: 'https://github.com/infolektuell/gradle-jextract/edit/main/docs/',
            },
            sidebar: [
                {
                    label: 'Getting Started',
                    autogenerate: { directory: 'start' },
                },
                {
                    label: 'Usage',
                    autogenerate: { directory: 'usage' },
                },
                {
                    label: 'Filtering',
                    autogenerate: { directory: 'filtering' },
                },
                {
                    label: 'Jextract Installation',
                    autogenerate: { directory: 'jextract-installation' },
                },
                {
                    label: 'Reference',
                    autogenerate: { directory: 'reference' },
                },
            ],
        }),
    ],
    devToolbar: {
        enabled: false,
    },
})
