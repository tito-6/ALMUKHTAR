import { NavGroup } from '@/types';

export const navGroups: NavGroup[] = [
  {
    label: 'مكتب العمليات',
    items: [
      {
        title: 'مركز القيادة',
        url: '/dashboard/overview',
        icon: 'dashboard',
        isActive: false,
        shortcut: ['d', 'd'],
        items: []
      },
      {
        title: 'الحوالات',
        url: '/dashboard/transfers',
        icon: 'send',
        shortcut: ['t', 't'],
        isActive: false,
        items: []
      },
      {
        title: 'المحافظ',
        url: '/dashboard/wallet',
        icon: 'creditCard',
        shortcut: ['w', 'w'],
        isActive: false,
        items: []
      },
      {
        title: 'الفروع والسيولة',
        url: '/dashboard/liquidity',
        icon: 'workspace',
        shortcut: ['l', 'l'],
        isActive: false,
        items: []
      },
      {
        title: 'إدارة الفروع',
        url: '/dashboard/branches',
        icon: 'workspace',
        shortcut: ['b', 'r'],
        isActive: false,
        items: []
      },
      {
        title: 'مساعد العمليات',
        url: '/dashboard/assistant',
        icon: 'chat',
        shortcut: ['a', 'a'],
        isActive: false,
        items: []
      }
    ]
  },
  {
    label: 'الخدمات المالية',
    items: [
      {
        title: 'الفواتير والخدمات',
        url: '/dashboard/services',
        icon: 'forms',
        isActive: false,
        items: []
      },
      {
        title: 'القروض الصغيرة',
        url: '/dashboard/lending',
        icon: 'billing',
        isActive: false,
        items: []
      },
      {
        title: 'خزينة الوثائق',
        url: '/dashboard/vault',
        icon: 'lock',
        isActive: false,
        items: []
      },
      {
        title: 'أسعار الصرف',
        url: '/dashboard/exchange-rates',
        icon: 'media',
        isActive: false,
        items: []
      },
      {
        title: 'منصة التداول',
        url: '/dashboard/trading',
        icon: 'billing',
        isActive: false,
        items: []
      }
    ]
  },
  {
    label: 'أدوات المستخدم',
    items: [
      {
        title: 'أهداف الادخار',
        url: '/dashboard/savings',
        icon: 'check',
        isActive: false,
        items: []
      },
      {
        title: 'التحويلات الدورية',
        url: '/dashboard/recurring',
        icon: 'post',
        isActive: false,
        items: []
      },
      {
        title: 'تقسيم المدفوعات',
        url: '/dashboard/split',
        icon: 'userPen',
        isActive: false,
        items: []
      },
      {
        title: 'عقود الضمان',
        url: '/dashboard/escrow',
        icon: 'lock',
        isActive: false,
        items: []
      },
      {
        title: 'حجوزات الدفع',
        url: '/dashboard/payout',
        icon: 'send',
        isActive: false,
        items: []
      },
      {
        title: 'المحفظة العائلية',
        url: '/dashboard/family',
        icon: 'teams',
        isActive: false,
        items: []
      },
      {
        title: 'الإحالة والمكافآت',
        url: '/dashboard/referral',
        icon: 'userPen',
        isActive: false,
        items: []
      },
      {
        title: 'نقاط الولاء',
        url: '/dashboard/gamification',
        icon: 'star',
        isActive: false,
        items: []
      }
    ]
  },
  {
    label: 'التجار والتسويق',
    items: [
      {
        title: 'التجار',
        url: '/dashboard/merchants',
        icon: 'store',
        isActive: false,
        items: []
      },
      {
        title: 'الحملات',
        url: '/dashboard/campaigns',
        icon: 'post',
        isActive: false,
        items: []
      }
    ]
  },
  {
    label: 'العمليات الفرعية',
    items: [
      {
        title: 'ورديات الصراف',
        url: '/dashboard/cashier',
        icon: 'userPen',
        isActive: false,
        items: []
      },
      {
        title: 'نقدية الفروع',
        url: '/dashboard/branch-cash',
        icon: 'workspace',
        isActive: false,
        items: []
      },
      {
        title: 'حوالات الدفعة',
        url: '/dashboard/batch',
        icon: 'send',
        isActive: false,
        items: []
      },
      {
        title: 'رموز QR',
        url: '/dashboard/qr',
        icon: 'media',
        isActive: false,
        items: []
      },
      {
        title: 'طلبات السحب (إدارة)',
        url: '/dashboard/cashout-admin',
        icon: 'creditCard',
        isActive: false,
        items: []
      },
      {
        title: 'طلبات التعبئة (إدارة)',
        url: '/dashboard/topup-admin',
        icon: 'creditCard',
        isActive: false,
        items: []
      }
    ]
  },
  {
    label: 'الرقابة والامتثال',
    items: [
      {
        title: 'تنبيهات AML',
        url: '/dashboard/aml',
        icon: 'flag',
        isActive: false,
        items: []
      },
      {
        title: 'الاعتراضات',
        url: '/dashboard/disputes',
        icon: 'flag',
        isActive: false,
        items: []
      },
      {
        title: 'تجميد الحسابات',
        url: '/dashboard/freeze',
        icon: 'lock',
        isActive: false,
        items: []
      },
      {
        title: 'سجل التدقيق',
        url: '/dashboard/audit',
        icon: 'post',
        isActive: false,
        items: []
      },
      {
        title: 'تحليلات الفروع',
        url: '/dashboard/analytics',
        icon: 'dashboard',
        isActive: false,
        items: []
      }
    ]
  },
  {
    label: 'الإدارة والرقابة',
    items: [
      {
        title: 'المستخدمون',
        url: '/dashboard/users',
        icon: 'teams',
        isActive: false,
        items: []
      },
      {
        title: 'الإشعارات',
        url: '/dashboard/notifications',
        icon: 'notification',
        isActive: false,
        items: []
      },
      {
        title: 'الحسابات المؤسسية',
        url: '/dashboard/corporate',
        icon: 'workspace',
        isActive: false,
        items: []
      },
      {
        title: 'إعداد الرسوم',
        url: '/dashboard/fee-config',
        icon: 'billing',
        isActive: false,
        items: []
      },
      {
        title: 'مناطق الرسوم',
        url: '/dashboard/fee-zones',
        icon: 'billing',
        isActive: false,
        items: []
      },
      {
        title: 'المستأجرون',
        url: '/dashboard/tenants',
        icon: 'settings',
        isActive: false,
        items: []
      },
      {
        title: 'المزامنة',
        url: '/dashboard/sync',
        icon: 'media',
        isActive: false,
        items: []
      }
    ]
  }
];
