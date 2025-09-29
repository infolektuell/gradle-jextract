import autoImport from 'astro-auto-import'

export default function starlightAutoImport(imports = []) {
    const components = ['Card', 'LinkCard', 'CardGrid', 'Tabs', 'TabItem', 'FileTree', 'Steps']
    return {
        name: 'auto-imports',
        hooks: {
            'config:setup': function ({ addIntegration }) {
                addIntegration(
                    autoImport({
                        imports: [
                            {
                                '@astrojs/starlight/components': components,
                            },
                            ...imports,
                        ],
                    }),
                )
            },
        },
    }
}
