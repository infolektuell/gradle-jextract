import { readFile } from 'node:fs/promises'

export const getLatestVersion = async function () {
    const text = await readFile('../release/version.txt', 'utf-8')
    return text.trim()
}
