import Cookies from 'js-cookie'

const tokenKey = () => window.location.pathname.startsWith('/merchant') ? 'Merchant-Token' : 'Admin-Token'

export function getToken() {
  return Cookies.get(tokenKey())
}

export function setToken(token) {
  return Cookies.set(tokenKey(), token, { expires: 7, sameSite: 'Lax' })
}

export function removeToken() {
  return Cookies.remove(tokenKey())
}
