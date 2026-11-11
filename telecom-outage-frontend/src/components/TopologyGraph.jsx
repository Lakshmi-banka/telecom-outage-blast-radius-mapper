import { useMemo } from 'react'

/**
 * Hand-rolled SVG dependency graph -- no charting library, so `npm install`
 * stays small and nothing can break on a version bump.
 *
 * edges:     [{ source, dependent }]   component codes
 * rootId:    the failed/root component code (drawn red)
 * affected:  Set/array of codes downstream of the root (drawn amber)
 *
 * Layout: nodes are placed in columns by their distance from a source node
 * (a node with no incoming edge), which matches how the backend's directed
 * topology actually flows: upstream -> downstream.
 */
export default function TopologyGraph({ edges = [], rootId, affected = [], height = 380 }) {
  const affectedSet = useMemo(() => new Set(affected), [affected])

  const { nodes, lines, width } = useMemo(() => {
    const ids = new Set()
    edges.forEach((e) => { ids.add(e.source); ids.add(e.dependent) })
    if (rootId) ids.add(rootId)
    const all = [...ids]
    if (all.length === 0) return { nodes: [], lines: [], width: 600 }

    const incoming = new Map(all.map((id) => [id, 0]))
    edges.forEach((e) => incoming.set(e.dependent, (incoming.get(e.dependent) || 0) + 1))

    // depth = longest path from any source, computed by relaxed BFS
    const depth = new Map(all.map((id) => [id, 0]))
    const sources = all.filter((id) => (incoming.get(id) || 0) === 0)
    const queue = [...(sources.length ? sources : [all[0]])]
    const guard = all.length * edges.length + all.length + 10
    let steps = 0
    while (queue.length && steps++ < guard) {
      const current = queue.shift()
      edges.filter((e) => e.source === current).forEach((e) => {
        const next = (depth.get(current) || 0) + 1
        if (next > (depth.get(e.dependent) || 0)) {
          depth.set(e.dependent, next)
          queue.push(e.dependent)
        }
      })
    }

    const byDepth = new Map()
    all.forEach((id) => {
      const d = depth.get(id) || 0
      if (!byDepth.has(d)) byDepth.set(d, [])
      byDepth.get(d).push(id)
    })

    const colGap = 190
    const rowGap = 74
    const maxDepth = Math.max(...byDepth.keys())
    const positioned = new Map()

    byDepth.forEach((group, d) => {
      group.sort()
      group.forEach((id, i) => {
        positioned.set(id, {
          id,
          x: 90 + d * colGap,
          y: 50 + i * rowGap + (group.length === 1 ? rowGap : 0),
        })
      })
    })

    const lineList = edges
      .map((e) => {
        const a = positioned.get(e.source)
        const b = positioned.get(e.dependent)
        if (!a || !b) return null
        return { key: `${e.source}->${e.dependent}`, a, b, source: e.source, dependent: e.dependent }
      })
      .filter(Boolean)

    return {
      nodes: [...positioned.values()],
      lines: lineList,
      width: Math.max(600, 180 + maxDepth * colGap),
    }
  }, [edges, rootId])

  if (nodes.length === 0) {
    return <p className="muted">No dependency data to draw.</p>
  }

  const tallest = Math.max(height, ...nodes.map((n) => n.y + 70))

  function tone(id) {
    if (id === rootId) return 'node-root'
    if (affectedSet.has(id)) return 'node-affected'
    return 'node-normal'
  }

  return (
    <div className="graph-scroll">
      <svg width={width} height={tallest} className="topology">
        <defs>
          <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5"
                  markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#5b6b86" />
          </marker>
        </defs>

        {lines.map((l) => {
          const hot = l.source === rootId || affectedSet.has(l.dependent)
          return (
            <line
              key={l.key}
              x1={l.a.x + 62} y1={l.a.y}
              x2={l.b.x - 62} y2={l.b.y}
              className={hot ? 'edge edge-hot' : 'edge'}
              markerEnd="url(#arrow)"
            />
          )
        })}

        {nodes.map((n) => (
          <g key={n.id} className={`node ${tone(n.id)}`}>
            <rect x={n.x - 62} y={n.y - 20} rx="8" width="124" height="40" />
            <text x={n.x} y={n.y + 5} textAnchor="middle">{n.id}</text>
          </g>
        ))}
      </svg>

      <div className="legend">
        <span><i className="dot dot-root" /> Root cause</span>
        <span><i className="dot dot-affected" /> Affected</span>
        <span><i className="dot dot-normal" /> Unaffected</span>
      </div>
    </div>
  )
}
