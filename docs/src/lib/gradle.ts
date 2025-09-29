import { z } from 'astro:content'
import { XMLParser } from 'fast-xml-parser'

const pluginMetadata = 'https://plugins.gradle.org/m2/de/infolektuell/gradle-plugin-jextract/maven-metadata.xml'

export const mavenSchema = z.object({
    metadata: z.object({
        version: z.string(),
    }),
})

export const fetchLatestVersion = async function (url: string = pluginMetadata): Promise<string> {
    const response = await fetch(url)
    if (!response.ok) {
        return 'x.y.z'
    }
    const xml = await response.text()
    const parser = new XMLParser()
    const json = parser.parse(xml)
    const data = mavenSchema.parse(json)
    return data.metadata.version
}
