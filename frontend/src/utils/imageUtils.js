/**
 * 处理图片URL，确保能正确显示
 * @param {string} url - 原始URL
 * @returns {string} - 处理后的URL
 */
export function processImageUrl(url) {
  if (!url) return "";

  // 如果已经是完整URL，直接返回
  if (url.startsWith("http://") || url.startsWith("https://")) {
    return url;
  }

  // 如果以/files/开头，直接返回
  if (url.startsWith("/files/")) {
    return url;
  }

  // 如果以/开头但不是/files/，添加/files
  if (url.startsWith("/")) {
    return `/files${url}`;
  }

  // 否则添加/files/前缀
  return `/files/${url}`;
}

/**
 * 处理媒体URL数组
 * @param {string|Array} mediaUrls - 媒体URL字符串（JSON）或数组
 * @returns {Array} - 处理后的URL数组
 */
export function processMediaUrls(mediaUrls) {
  if (!mediaUrls) return [];

  let urls = [];
  try {
    if (typeof mediaUrls === "string") {
      urls = JSON.parse(mediaUrls);
    } else if (Array.isArray(mediaUrls)) {
      urls = mediaUrls;
    }
  } catch {
    return [];
  }

  return urls.map(processImageUrl);
}

