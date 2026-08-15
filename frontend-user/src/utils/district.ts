/**
 * 从收货地址详情字符串中提取区县名
 *
 * 数据约定：detail 以区县名开头，如「朝阳区望京SOHO T1-1201」→「朝阳区」。
 * 无区县时返回空字符串，由调用方决定是否展示。
 */
export const extractDistrict = (detail?: string | null): string => {
  if (!detail) return ''
  const match = detail.match(/^([\u4e00-\u9fa5]{2,4}?[区县市])/)
  return match ? match[1] : ''
}
