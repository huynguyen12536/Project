export type CatalogCourse = {
  id: number;
  title: string;
  instructor: string;
  category: string;
  rating: string;
  reviews: string;
  price: string;
  oldPrice: string;
  color: string;
  badge: 'Ban chay' | 'Cao cap';
  updatedLabel: string;
  totalHours: string;
  level: string;
  summary: string;
  highlights: string[];
};

export type InstructorLectureType = 'video' | 'article' | 'quiz';

export type InstructorLecture = {
  id: string;
  title: string;
  duration: string;
  type: InstructorLectureType;
  previewable?: boolean;
};

export type InstructorSection = {
  id: string;
  title: string;
  lectures: InstructorLecture[];
};

export type InstructorCourse = {
  id: string;
  title: string;
  category: string;
  status: 'Dang soan thao' | 'Da xuat ban' | 'Dang cho duyet';
  updatedAt: string;
  students: string;
  revenue: string;
  rating: string;
  subtitle: string;
  curriculum: InstructorSection[];
};

export const catalogCourses: CatalogCourse[] = [
  {
    id: 1,
    title: 'Lap trinh Web Full-Stack 2026',
    instructor: 'Tran Minh Quan',
    category: 'Lap trinh',
    rating: '4.8',
    reviews: '12.430',
    price: '499.000d',
    oldPrice: '899.000d',
    color: '#2F2FA2',
    badge: 'Ban chay',
    updatedLabel: 'Cap nhat thang 6 nam 2026',
    totalHours: 'Tong 20 gio',
    level: 'Tat ca trinh do',
    summary:
      'Xay dung ung dung web hoan chinh voi React, Node.js, PostgreSQL va quy trinh deploy theo cach lam viec thuc te.',
    highlights: [
      'Lam duoc auth flow, dashboard va player cho nen tang hoc tap',
      'Thiet ke API, database va cau truc monorepo co the mo rong',
      'Trien khai du an len moi truong that voi CI/CD co ban',
      'Doc va sua duoc source code theo phong cach san pham',
    ],
  },
  {
    id: 2,
    title: 'UI/UX Design tu A den Z',
    instructor: 'Le Thu Ha',
    category: 'Thiet ke',
    rating: '4.7',
    reviews: '8.210',
    price: '349.000d',
    oldPrice: '699.000d',
    color: '#F64C72',
    badge: 'Cao cap',
    updatedLabel: 'Cap nhat thang 5 nam 2026',
    totalHours: 'Tong 14 gio',
    level: 'Tat ca trinh do',
    summary:
      'Di tu wireframe, user flow den design system hoan chinh cho web app, dashboard va quy trinh handoff cho dev.',
    highlights: [
      'Xay bo component va token dung duoc cho nhieu man hinh',
      'Hoc cach danh uu tien UX theo hanh vi nguoi hoc that',
      'Lam prototype sat voi san pham SaaS hien dai',
      'Toi uu giao dien de tang toc do thao tac va giam loi',
    ],
  },
  {
    id: 3,
    title: 'Phan tich du lieu voi Python',
    instructor: 'Pham Duc Anh',
    category: 'Du lieu',
    rating: '4.9',
    reviews: '15.800',
    price: '599.000d',
    oldPrice: '999.000d',
    color: '#553D67',
    badge: 'Ban chay',
    updatedLabel: 'Cap nhat thang 4 nam 2026',
    totalHours: 'Tong 22 gio',
    level: 'Tat ca trinh do',
    summary:
      'Tien xu ly, phan tich va truc quan hoa du lieu bang Pandas, NumPy va notebook workflow de ra quyet dinh nhanh hon.',
    highlights: [
      'Lam sach du lieu tu dau vao thuc te thay vi bo data dep san',
      'Tao dashboard va bao cao du lieu phuc vu stakeholder',
      'Doc duoc logic phan tich de sua loi va mo rong ve sau',
      'Xay tu duy dat cau hoi dung truoc khi viet code',
    ],
  },
  {
    id: 4,
    title: 'React va Redux Toolkit',
    instructor: 'Nguyen Hoang',
    category: 'Frontend',
    rating: '4.6',
    reviews: '5.640',
    price: '449.000d',
    oldPrice: '799.000d',
    color: '#242582',
    badge: 'Cao cap',
    updatedLabel: 'Cap nhat thang 6 nam 2026',
    totalHours: 'Tong 16 gio',
    level: 'Trung cap',
    summary:
      'Dao sau vao state management, route architecture, async flow va cach giu frontend lon van de bao tri theo thoi gian.',
    highlights: [
      'To chuc store gon va de test cho ung dung lon',
      'Xu ly loading, optimistic UI va skeleton dung ngu canh',
      'Noi du lieu backend vao giao dien khong gay xao tron state',
      'Lam duoc frontend pattern phu hop cho SaaS va EdTech',
    ],
  },
  {
    id: 5,
    title: 'Digital Marketing toan dien',
    instructor: 'Vu Lan Anh',
    category: 'Marketing',
    rating: '4.5',
    reviews: '9.120',
    price: '299.000d',
    oldPrice: '599.000d',
    color: '#99738E',
    badge: 'Ban chay',
    updatedLabel: 'Cap nhat thang 3 nam 2026',
    totalHours: 'Tong 12 gio',
    level: 'Tat ca trinh do',
    summary:
      'Tong hop cac kenh digital, funnel, content va cach do hieu qua chien dich de tang chuyen doi theo du lieu.',
    highlights: [
      'Lap duoc ke hoach campaign ngan sach nho den vua',
      'Doc duoc chi so de phan biet kenh nao tao hieu qua that',
      'Toi uu noi dung va diem cham chuyen doi tren landing page',
      'Noi digital voi muc tieu tang truong san pham',
    ],
  },
  {
    id: 6,
    title: 'He thong thiet ke cho SaaS Product',
    instructor: 'Le Thu Ha',
    category: 'Thiet ke',
    rating: '4.8',
    reviews: '6.750',
    price: '399.000d',
    oldPrice: '749.000d',
    color: '#F64C72',
    badge: 'Cao cap',
    updatedLabel: 'Cap nhat thang 6 nam 2026',
    totalHours: 'Tong 18 gio',
    level: 'Trung cap',
    summary:
      'Tap trung vao thong nhat giao dien, interaction va quy tac tai su dung cho product team lam viec nhanh va chac.',
    highlights: [
      'Tao duoc foundation token, pattern va guideline hoat dong that',
      'Dong bo component giua designer va frontend de giam lech',
      'Mo rong he thong theo nhieu scenario san pham',
      'Giup toc do release nhanh hon ma van giu chat luong',
    ],
  },
];

export const instructorCoursesSeed: InstructorCourse[] = [
  {
    id: 'ins-course-1',
    title: 'Lap trinh Web Full-Stack 2026',
    category: 'Lap trinh',
    status: 'Da xuat ban',
    updatedAt: '2026-06-29',
    students: '12.4K',
    revenue: '248.4tr',
    rating: '4.8',
    subtitle: 'Khoa hoc chu luc giup hoc vien di tu auth den dashboard va deploy.',
    curriculum: [
      {
        id: 'section-1',
        title: 'Chuong 1 · Gioi thieu va moi truong',
        lectures: [
          { id: 'lecture-1', title: 'Tong quan lo trinh khoa hoc', duration: '06:12', type: 'video', previewable: true },
          { id: 'lecture-2', title: 'Cai dat cong cu va monorepo', duration: '11:40', type: 'video' },
          { id: 'lecture-3', title: 'Checklist bat dau du an', duration: '05:30', type: 'article' },
        ],
      },
      {
        id: 'section-2',
        title: 'Chuong 2 · Frontend architecture',
        lectures: [
          { id: 'lecture-4', title: 'Routing, layout va flow dang nhap', duration: '14:25', type: 'video' },
          { id: 'lecture-5', title: 'Quiz giua module', duration: '08 cau hoi', type: 'quiz' },
        ],
      },
    ],
  },
  {
    id: 'ins-course-2',
    title: 'He thong thiet ke cho SaaS Product',
    category: 'Thiet ke',
    status: 'Dang cho duyet',
    updatedAt: '2026-06-30',
    students: '1.1K',
    revenue: '38.7tr',
    rating: '4.9',
    subtitle: 'Huong dan xay design system co the ban giao thang sang frontend team.',
    curriculum: [
      {
        id: 'section-3',
        title: 'Chuong 1 · Nen tang design system',
        lectures: [
          { id: 'lecture-6', title: 'Color, type va spacing scale', duration: '09:45', type: 'video' },
          { id: 'lecture-7', title: 'Token naming guide', duration: '04:10', type: 'article' },
        ],
      },
    ],
  },
  {
    id: 'ins-course-3',
    title: 'AI Prompting thuc chien cho team van hanh',
    category: 'AI',
    status: 'Dang soan thao',
    updatedAt: '2026-06-27',
    students: '0',
    revenue: '0d',
    rating: '-',
    subtitle: 'Khoa hoc moi dang duoc xay de giup team van hanh lam viec nhanh va ro hon.',
    curriculum: [
      {
        id: 'section-4',
        title: 'Chuong 1 · Khung prompt co the tai su dung',
        lectures: [
          { id: 'lecture-8', title: 'Prompt framework cho tac vu lap lai', duration: '07:20', type: 'video' },
        ],
      },
    ],
  },
];
