import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'
import type { UserType } from '@/types'

export const vPermission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<UserType[]>) {
    const userStore = useUserStore()
    const requiredRoles = binding.value
    if (requiredRoles && !requiredRoles.includes(userStore.userType as UserType)) {
      el.parentNode?.removeChild(el)
    }
  },
}
