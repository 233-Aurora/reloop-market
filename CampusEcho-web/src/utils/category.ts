import type { CategoryTreeResponse } from '../api/types'

export interface CategoryOption {
  value: number
  label: string
  children?: CategoryOption[]
  disabled?: boolean
}

export interface FlatCategory {
  id: number
  name: string
  parentId: number | null
  depth: number
  path: string
  childrenCount: number
}

export function toCategoryOptions(nodes: CategoryTreeResponse[]): CategoryOption[] {
  return nodes.map((node) => ({
    value: node.id,
    label: node.name,
    children: node.children?.length ? toCategoryOptions(node.children) : undefined,
  }))
}

export function findCategoryName(
  nodes: CategoryTreeResponse[],
  id: number,
): string | null {
  for (const node of nodes) {
    if (node.id === id) return node.name
    if (node.children?.length) {
      const found = findCategoryName(node.children, id)
      if (found) return found
    }
  }
  return null
}

export function findCategoryNode(
  nodes: CategoryTreeResponse[],
  id: number,
): CategoryTreeResponse | null {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children?.length) {
      const found = findCategoryNode(node.children, id)
      if (found) return found
    }
  }
  return null
}

/** 收集某节点及其全部后代 id（含自身） */
export function collectSelfAndDescendantIds(
  nodes: CategoryTreeResponse[],
  id: number,
): Set<number> {
  const result = new Set<number>()
  const node = findCategoryNode(nodes, id)
  if (!node) return result

  function walk(current: CategoryTreeResponse) {
    result.add(current.id)
    current.children?.forEach(walk)
  }
  walk(node)
  return result
}

export function flattenCategoryTree(
  nodes: CategoryTreeResponse[],
  parentId: number | null = null,
  depth = 0,
  parentPath = '',
): FlatCategory[] {
  const rows: FlatCategory[] = []
  for (const node of nodes) {
    const path = parentPath ? `${parentPath} / ${node.name}` : node.name
    rows.push({
      id: node.id,
      name: node.name,
      parentId,
      depth,
      path,
      childrenCount: node.children?.length ?? 0,
    })
    if (node.children?.length) {
      rows.push(...flattenCategoryTree(node.children, node.id, depth + 1, path))
    }
  }
  return rows
}

/** 父级选择：排除自身及后代，避免循环 */
export function toParentOptions(
  nodes: CategoryTreeResponse[],
  excludeId?: number | null,
): CategoryOption[] {
  const blocked = excludeId != null ? collectSelfAndDescendantIds(nodes, excludeId) : new Set<number>()

  function mapNodes(list: CategoryTreeResponse[]): CategoryOption[] {
    return list
      .filter((node) => !blocked.has(node.id))
      .map((node) => ({
        value: node.id,
        label: node.name,
        children: node.children?.length ? mapNodes(node.children) : undefined,
      }))
  }

  return mapNodes(nodes)
}
