export function orderPrizeSaves(prizes) {
  const contribution = (probability, enabled) => enabled === false ? 0 : Number(probability || 0)
  return prizes
    .map((prize, index) => ({
      prize,
      index,
      delta: contribution(prize.probability, prize.enabled) - contribution(
        prize._originalProbability,
        Object.hasOwn(prize, '_originalEnabled') ? prize._originalEnabled : prize.enabled
      )
    }))
    .sort((left, right) => left.delta - right.delta || left.index - right.index)
    .map(({ prize }) => prize)
}

export async function savePrizePool(prizes, savePrize) {
  const ordered = orderPrizeSaves(prizes)
  for (const prize of ordered) await savePrize(prize)
  return ordered
}

export function toRulePayload(rules) {
  return rules
    .filter((rule) => rule.content?.trim())
    .map((rule, sortOrder) => ({
      ...(rule.id ? { id: rule.id } : {}),
      content: rule.content.trim(),
      enabled: rule.enabled !== false,
      sortOrder
    }))
}
