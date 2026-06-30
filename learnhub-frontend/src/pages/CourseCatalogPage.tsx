import { useNavigate } from 'react-router-dom';

const CourseCatalogPage = () => {
  const navigate = useNavigate();

  const courses = [
    { id: 1, title: 'Lập trình Web Full-Stack 2026', instr: 'Trần Minh Quân', cat: 'Lập trình', rating: '4.8', reviews: '12.430', price: '499.000₫', old: '899.000₫', color: '#2F2FA2', best: true },
    { id: 2, title: 'UI/UX Design từ A đến Z', instr: 'Lê Thu Hà', cat: 'Thiết kế', rating: '4.7', reviews: '8.210', price: '349.000₫', old: '699.000₫', color: '#F64C72', best: false },
    { id: 3, title: 'Phân tích dữ liệu với Python', instr: 'Phạm Đức Anh', cat: 'Dữ liệu', rating: '4.9', reviews: '15.800', price: '599.000₫', old: '999.000₫', color: '#553D67', best: true },
    { id: 4, title: 'React & Redux Toolkit', instr: 'Nguyễn Hoàng', cat: 'Lập trình', rating: '4.6', reviews: '5.640', price: '449.000₫', old: '799.000₫', color: '#242582', best: false },
  ];

  const categories = [
    { name: 'Lập trình', count: '12.400' },
    { name: 'Thiết kế', count: '8.100' },
    { name: 'Kinh doanh', count: '6.700' },
    { name: 'Marketing', count: '5.300' },
    { name: 'Dữ liệu & AI', count: '4.900' },
    { name: 'Nhiếp ảnh', count: '3.200' },
  ];

  return (
    <div className="min-h-screen bg-white">
      <div className="max-w-6xl mx-auto px-8 py-8">
        <div className="text-xs text-lh-muted font-semibold mb-3">
          <button onClick={() => navigate('/')} className="cursor-pointer hover:text-lh-navy">Trang chủ</button> › <span className="text-lh-navy">Tất cả khoá học</span>
        </div>
        <h1 className="text-4xl font-black mb-2">Tất cả khoá học</h1>
        <p className="text-base text-lh-muted mb-8">8 khoá học</p>

        <div className="flex gap-8 items-start">
          {/* Sidebar Filters */}
          <aside className="w-64 flex-none">
            <div className="border border-lh-border bg-white">
              {/* Category Filter */}
              <div className="px-5 py-4 border-b border-[#EFF1F7]">
                <div className="text-xs font-black uppercase tracking-wider text-lh-muted mb-3">Danh mục</div>
                <div className="flex flex-col gap-3">
                  {categories.map((cat) => (
                    <label key={cat.name} className="flex items-center gap-2 text-sm font-semibold text-lh-navy cursor-pointer">
                      <input type="checkbox" className="w-4 h-4 border border-lh-input" />
                      {cat.name} <span className="text-lh-muted ml-auto text-xs">{cat.count}</span>
                    </label>
                  ))}
                </div>
              </div>

              {/* Level Filter */}
              <div className="px-5 py-4 border-b border-[#EFF1F7]">
                <div className="text-xs font-black uppercase tracking-wider text-lh-muted mb-3">Trình độ</div>
                <div className="flex flex-col gap-3">
                  {['Tất cả trình độ', 'Cơ bản', 'Trung cấp', 'Nâng cao'].map((level) => (
                    <label key={level} className="flex items-center gap-2 text-sm font-semibold text-lh-navy cursor-pointer">
                      <input type="radio" name="level" className="w-4 h-4" />
                      {level}
                    </label>
                  ))}
                </div>
              </div>

              {/* Rating Filter */}
              <div className="px-5 py-4">
                <div className="text-xs font-black uppercase tracking-wider text-lh-muted mb-3">Đánh giá</div>
                <div className="flex flex-col gap-3">
                  <label className="flex items-center gap-2 text-sm font-semibold text-lh-navy cursor-pointer">
                    <input type="radio" name="rating" className="w-4 h-4 border-4 border-lh-blue" />
                    <span className="text-[#F6A609]">★★★★★</span> 4.5+
                  </label>
                  <label className="flex items-center gap-2 text-sm font-semibold text-lh-navy cursor-pointer">
                    <input type="radio" name="rating" className="w-4 h-4" />
                    <span className="text-[#F6A609]">★★★★</span>☆ 4.0+
                  </label>
                </div>
              </div>
            </div>
          </aside>

          {/* Results Grid */}
          <div className="flex-1">
            <div className="flex items-center justify-between mb-6 pb-4 border-b border-[#EFF1F7]">
              <span className="text-sm text-lh-muted font-semibold">8 khoá học</span>
              <div className="flex items-center gap-2">
                <span className="text-sm text-lh-muted font-semibold">Sắp xếp:</span>
                <button className="flex items-center gap-2 h-10 px-3.5 border border-lh-input text-sm font-bold text-lh-navy rounded cursor-pointer">
                  Phổ biến nhất
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M6 9l6 6 6-6" />
                  </svg>
                </button>
              </div>
            </div>

            <div className="grid grid-cols-3 gap-5">
              {courses.map((c) => (
                <button key={c.id} onClick={() => navigate(`/courses/${c.id}`)} className="bg-white border border-lh-border rounded-2xl overflow-hidden hover:border-lh-muted text-left">
                  <div className="h-36 relative flex items-center justify-center" style={{ backgroundColor: c.color }}>
                    <div className="w-0 h-0 border-t-6 border-b-6 border-l-10 border-t-transparent border-b-transparent border-l-white ml-1"></div>
                    {c.best && <span className="absolute top-2 left-2 bg-lh-pink text-white text-[10px] font-black px-2 py-1">BESTSELLER</span>}
                  </div>
                  <div className="p-4">
                    <div className="text-base font-black mb-1 line-clamp-2">{c.title}</div>
                    <div className="text-sm text-lh-muted mb-2">{c.instr} · {c.cat}</div>
                    <div className="flex items-center gap-1 mb-2"><span className="text-amber-700 text-sm font-black">{c.rating}</span><span className="text-[#F6A609]">★★★★★</span><span className="text-xs text-lh-muted">({c.reviews})</span></div>
                    <div className="flex items-center gap-2"><span className="text-xl font-black text-lh-navy">{c.price}</span><span className="text-sm text-lh-muted line-through">{c.old}</span></div>
                  </div>
                </button>
              ))}
            </div>

            {/* Pagination */}
            <div className="flex justify-center gap-2 mt-9">
              <button className="w-11 h-11 border border-lh-input bg-white font-bold text-lh-muted rounded cursor-pointer">‹</button>
              <button className="w-11 h-11 border-none bg-lh-navy text-white font-bold rounded cursor-pointer">1</button>
              <button className="w-11 h-11 border border-lh-input bg-white font-bold text-lh-navy rounded cursor-pointer">2</button>
              <button className="w-11 h-11 border border-lh-input bg-white font-bold text-lh-navy rounded cursor-pointer">3</button>
              <button className="w-11 h-11 border border-lh-input bg-white font-bold text-lh-navy rounded cursor-pointer">›</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CourseCatalogPage;
