const serialise = (model) => JSON.stringify(model)

/**
 * Stores the last confirmed configuration state.  Call capture after the
 * overview DTO arrives (or after a successful save), never before.
 */
export function useDirtySnapshot(model) {
  let snapshot = ''

  const capture = () => { snapshot = serialise(model) }
  const isDirty = () => snapshot !== '' && serialise(model) !== snapshot
  const reset = () => capture()

  return { capture, isDirty, reset }
}
