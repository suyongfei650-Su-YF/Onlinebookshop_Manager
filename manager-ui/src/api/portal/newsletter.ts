import { userRequest } from '../user/http'

export function portalNewsletterSubscribe(email: string) {
  return userRequest<{ message: string }>('/portal/newsletter/subscribe', {
    method: 'POST',
    body: JSON.stringify({ email: email.trim() }),
  })
}
