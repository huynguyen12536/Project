import { useNavigate } from 'react-router-dom';

const HomePage = () => {
  const navigate = useNavigate();

  const courses = [
    { id: 1, title: 'Lập trình Web Full-Stack 2026', instr: 'Trần Minh Quân', cat: 'Lập trình', rating: '4.8', reviews: '12.430', price: '499.000₫', old: '899.000₫', color: '#2F2FA2', best: true },
    { id: 2, title: 'UI/UX Design từ A đến Z', instr: 'Lê Thu Hà', cat: 'Thiết kế', rating: '4.7', reviews: '8.210', price: '349.000₫', old: '699.000₫', color: '#F64C72', best: false },
    { id: 3, title: 'Phân tích dữ liệu với Python', instr: 'Phạm Đức Anh', cat: 'Dữ liệu', rating: '4.9', reviews: '15.800', price: '599.000₫', old: '999.000₫', color: '#553D67', best: true },
    { id: 4, title: 'React & Redux Toolkit', instr: 'Nguyễn Hoàng', cat: 'Lập trình', rating: '4.6', reviews: '5.640', price: '449.000₫', old: '799.000₫', color: '#242582', best: false },
    { id: 5, title: 'Digital Marketing toàn diện', instr: 'Vũ Lan Anh', cat: 'Marketing', rating: '4.5', reviews: '9.120', price: '299.000₫', old: '599.000₫', color: '#99738E', best: false },
    { id: 6, title: 'Thiết kế đồ hoạ với Figma', instr: 'Đỗ Khánh', cat: 'Thiết kế', rating: '4.8', reviews: '6.750', price: '399.000₫', old: '749.000₫', color: '#F64C72', best: false },
    { id: 7, title: 'Machine Learning cơ bản', instr: 'Bùi Tuấn', cat: 'Dữ liệu', rating: '4.9', reviews: '11.200', price: '699.000₫', old: '1.199.000₫', color: '#2F2FA2', best: true },
    { id: 8, title: 'Tiếng Anh giao tiếp công sở', instr: 'Mai Phương', cat: 'Ngôn ngữ', rating: '4.7', reviews: '18.400', price: '259.000₫', old: '499.000₫', color: '#553D67', best: false },
  ];

  const categories = [
    { name: 'Lập trình', count: '12.400', color: '#2F2FA2' },
    { name: 'Thiết kế', count: '8.100', color: '#F64C72' },
    { name: 'Kinh doanh', count: '6.700', color: '#553D67' },
    { name: 'Marketing', count: '5.300', color: '#99738E' },
    { name: 'Dữ liệu & AI', count: '4.900', color: '#242582' },
    { name: 'Nhiếp ảnh', count: '3.200', color: '#2F2FA2' },
    { name: 'Âm nhạc', count: '2.800', color: '#F64C72' },
    { name: 'Ngôn ngữ', count: '4.100', color: '#553D67' },
  ];

  const instructors = [
    { name: 'Trần Minh Quân', role: 'Senior Full-Stack Engineer', initials: 'TQ', rating: '4.8', students: '73.2K', color: '#2F2FA2' },
    { name: 'Lê Thu Hà', role: 'Lead Product Designer', initials: 'LH', rating: '4.7', students: '41.5K', color: '#F64C72' },
    { name: 'Phạm Đức Anh', role: 'Data Scientist', initials: 'PA', rating: '4.9', students: '58.9K', color: '#553D67' },
    { name: 'Vũ Lan Anh', role: 'Marketing Director', initials: 'VA', rating: '4.5', students: '32.7K', color: '#99738E' },
  ];

  const testimonials = [
    { quote: 'Khoá Full-Stack đã thay đổi hoàn toàn sự nghiệp của mình. Từ con số 0 đến khi đi làm lập trình chỉ trong 6 tháng.', name: 'Lê Văn Hùng', role: 'Frontend Developer', initials: 'LH', color: '#2F2FA2' },
    { quote: 'Giảng giải dễ hiểu, dự án thực tế, và giảng viên trả lời câu hỏi tận tình. Rất xứng đáng.', name: 'Trần Thị My', role: 'UX Designer', initials: 'TM', color: '#F64C72' },
    { quote: 'Mình học trên đường đi làm bằng app. Truy cập trọn đời nên lúc nào cần ôn lại đều được.', name: 'Phạm Đức Long', role: 'Data Analyst', initials: 'PL', color: '#553D67' },
  ];

  return (
    <div className="min-h-screen">
      {/* Hero */}
      <section className="bg-lh-navy text-white py-0 px-0 relative overflow-hidden">
        <div className="max-w-6xl mx-auto px-8 py-20 grid grid-cols-2 gap-12 items-center relative">
          <div>
            <div className="text-xs font-bold tracking-wider text-[#99738E] mb-4 uppercase">NỀN TẢNG HỌC TRỰC TUYẾN</div>
            <h1 className="text-5xl font-black leading-tight mb-5 tracking-tight">Học kỹ năng thật, từ chuyên gia thật</h1>
            <p className="text-lg text-[#C9C8E8] mb-7 max-w-lg">Hơn 210.000 khoá học video chất lượng cao về lập trình, thiết kế, kinh doanh và hơn thế nữa. Học theo nhịp của riêng bạn.</p>

            <div className="flex items-center gap-2 bg-white text-lh-navy rounded-full p-1 max-w-lg mb-7 shadow-2xl hover:shadow-3xl">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="ml-4 flex-none text-gray-400">
                <circle cx="11" cy="11" r="7" />
                <path d="M21 21l-4-4" />
              </svg>
              <input placeholder="Tìm khoá học bạn muốn học…" className="flex-1 border-none outline-none bg-transparent text-sm" />
              <button onClick={() => navigate('/courses')} className="h-12 px-7 bg-lh-pink text-white font-bold text-sm rounded-full flex-none hover:bg-lh-pink-dark">
                Tìm
              </button>
            </div>

            <div className="flex gap-3 mb-9">
              <button onClick={() => navigate('/courses')} className="h-14 px-7 bg-transparent text-white border border-white/35 rounded-full font-bold text-base hover:border-white hover:bg-white/8">
                Khám phá khoá học
              </button>
              <button className="h-14 px-6 bg-transparent text-white border-none font-bold text-base flex items-center gap-2 hover:text-[#F9B8C7]">
                <span className="w-10 h-10 rounded-full bg-lh-pink flex items-center justify-center flex-none">
                  <div className="w-0 h-0 border-t-[5px] border-b-[5px] border-l-[8px] border-t-transparent border-b-transparent border-l-white ml-1"></div>
                </span>
                Dùng thử miễn phí
              </button>
            </div>

            <div className="flex gap-10">
              <div><div className="text-4xl font-black tracking-tight">210K+</div><div className="text-sm text-[#99738E] font-semibold">Khoá học</div></div>
              <div><div className="text-4xl font-black tracking-tight">73tr</div><div className="text-sm text-[#99738E] font-semibold">Học viên</div></div>
              <div><div className="text-4xl font-black tracking-tight">4.7/5</div><div className="text-sm text-[#99738E] font-semibold">Đánh giá</div></div>
            </div>
          </div>

          <div className="relative h-96">
            <div className="absolute right-0 top-4 w-56 h-56 rounded-full bg-[#2F2FA2]"></div>
            <div className="absolute right-40 -top-2 w-24 h-24 rounded-full bg-lh-pink"></div>
            <div className="absolute -right-2 bottom-12 z-10 flex items-center gap-3 bg-white text-lh-dark rounded-full p-2.5 pr-5 shadow-2xl">
              <span className="w-12 h-12 rounded-full bg-[#EEF0FB] text-lh-blue flex items-center justify-center flex-none text-lg font-bold">73tr+</span>
              <div><div className="text-2xl font-black tracking-tight text-lh-navy">73tr+</div><div className="text-xs text-gray-500 font-semibold">Học viên đang học</div></div>
            </div>
            <div className="absolute left-0 bottom-0 w-72 bg-white text-lh-dark p-4 rounded-2xl shadow-3xl">
              <div className="h-32 bg-[#553D67] flex items-center justify-center mb-3 relative rounded-lg">
                <div className="w-0 h-0 border-t-7 border-b-7 border-l-12 border-t-transparent border-b-transparent border-l-white ml-1"></div>
                <span className="absolute top-2 left-2 bg-lh-pink text-white text-xs font-black px-2 py-1">BESTSELLER</span>
              </div>
              <div className="text-base font-black mb-1">Lập trình Web Full-Stack 2026</div>
              <div className="text-sm text-gray-500 mb-2">Trần Minh Quân</div>
              <div className="flex gap-1 mb-3"><span className="text-[#F6A609] font-bold text-sm">4.8</span><span className="text-[#F6A609]">★★★★★</span><span className="text-xs text-gray-500">(12.430)</span></div>
              <div className="flex justify-between"><span className="text-2xl font-black text-lh-navy">499.000₫</span><span className="text-sm text-gray-400 line-through">899.000₫</span></div>
            </div>
          </div>
        </div>
      </section>

      {/* Stats */}
      <section className="max-w-6xl mx-auto px-8 py-12">
        <div className="grid grid-cols-4 gap-6">
          <div className="flex flex-col items-center text-center gap-3">
            <span className="w-20 h-20 rounded-full bg-[#EEF0FB] text-lh-blue flex items-center justify-center text-3xl">📚</span>
            <div><div className="text-4xl font-black text-lh-navy">210K+</div><div className="text-sm text-lh-muted font-semibold">Khoá học trực tuyến</div></div>
          </div>
          <div className="flex flex-col items-center text-center gap-3">
            <span className="w-20 h-20 rounded-full bg-[#FDE7EC] text-lh-pink flex items-center justify-center text-3xl">🎓</span>
            <div><div className="text-4xl font-black text-lh-navy">1.240</div><div className="text-sm text-lh-muted font-semibold">Chương trình đào tạo</div></div>
          </div>
          <div className="flex flex-col items-center text-center gap-3">
            <span className="w-20 h-20 rounded-full bg-[#F0EAF3] text-lh-purple flex items-center justify-center text-3xl">🖥️</span>
            <div><div className="text-4xl font-black text-lh-navy">684K</div><div className="text-sm text-lh-muted font-semibold">Học viên có chứng chỉ</div></div>
          </div>
          <div className="flex flex-col items-center text-center gap-3">
            <span className="w-20 h-20 rounded-full bg-[#FDF1E3] text-amber-700 flex items-center justify-center text-3xl">👥</span>
            <div><div className="text-4xl font-black text-lh-navy">73tr</div><div className="text-sm text-lh-muted font-semibold">Lượt ghi danh</div></div>
          </div>
        </div>
      </section>

      {/* Journey */}
      <section className="max-w-6xl mx-auto px-8 py-16">
        <div className="text-center max-w-2xl mx-auto mb-9">
          <h2 className="text-3xl font-black mb-3">Bắt đầu hành trình cùng LearnHub</h2>
          <p className="text-base text-lh-muted">Một cách tiếp cận mới cho việc học — chọn lộ trình phù hợp và làm chủ kỹ năng mới mỗi ngày.</p>
        </div>
        <div className="grid grid-cols-4 gap-5">
          {[
            { num: '01', title: 'Giảng viên chuyên gia', desc: 'Học trực tiếp từ những người đang làm nghề, giàu kinh nghiệm thực chiến.' },
            { num: '02', title: 'Nội dung chất lượng', desc: 'Khoá học được kiểm duyệt kỹ, cập nhật và bám sát thực tế ngành.' },
            { num: '03', title: 'Học từ xa linh hoạt', desc: 'Học mọi lúc, mọi nơi, trên web và ứng dụng di động.' },
            { num: '04', title: 'Hỗ trợ trọn đời', desc: 'Truy cập trọn đời và được hỗ trợ hỏi đáp sau khoá học.' },
          ].map((item) => (
            <div key={item.num} className="bg-white border border-lh-border rounded-2xl p-7 hover:shadow-lg hover:-translate-y-1 transition">
              <div className="text-5xl font-black text-[#EBECF6] mb-3">{item.num}</div>
              <h3 className="text-lg font-black mb-2">{item.title}</h3>
              <p className="text-sm text-lh-muted">{item.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Category Chips */}
      <section className="max-w-6xl mx-auto px-8 py-8">
        <div className="flex flex-wrap gap-3">
          {categories.map((cat) => (
            <button key={cat.name} onClick={() => navigate('/courses')} className="h-11 px-5 bg-white border border-lh-input rounded-lg font-bold text-sm text-lh-navy hover:border-lh-blue hover:text-lh-blue">
              {cat.name}
            </button>
          ))}
        </div>
      </section>

      {/* Popular Courses */}
      <section className="max-w-6xl mx-auto px-8 py-12">
        <div className="flex items-end justify-between mb-6">
          <div>
            <h2 className="text-3xl font-black mb-2">Khoá học phổ biến</h2>
            <p className="text-base text-lh-muted">Được hàng nghìn học viên lựa chọn trong tháng này.</p>
          </div>
          <button onClick={() => navigate('/courses')} className="text-sm font-bold text-lh-blue hover:text-lh-navy">Xem tất cả →</button>
        </div>
        <div className="grid grid-cols-4 gap-5">
          {courses.map((c) => (
            <button key={c.id} onClick={() => navigate(`/courses/${c.id}`)} className="bg-white border border-lh-border rounded-2xl overflow-hidden hover:shadow-lg hover:-translate-y-1 transition text-left">
              <div className="h-32 relative flex items-center justify-center" style={{ backgroundColor: c.color }}>
                <div className="w-0 h-0 border-t-5 border-b-5 border-l-8 border-t-transparent border-b-transparent border-l-white ml-1"></div>
                {c.best && <span className="absolute top-2 left-2 bg-lh-pink text-white text-[10px] font-black px-2 py-1">BESTSELLER</span>}
              </div>
              <div className="p-4">
                <div className="text-base font-black mb-1 line-clamp-2">{c.title}</div>
                <div className="text-sm text-lh-muted mb-2">{c.instr}</div>
                <div className="flex items-center gap-1 mb-2"><span className="text-amber-700 text-sm font-black">{c.rating}</span><span className="text-[#F6A609]">★★★★★</span><span className="text-xs text-lh-muted">({c.reviews})</span></div>
                <div className="flex items-center gap-2"><span className="text-xl font-black text-lh-navy">{c.price}</span><span className="text-sm text-lh-muted line-through">{c.old}</span></div>
              </div>
            </button>
          ))}
        </div>
      </section>

      {/* Top Categories */}
      <section className="bg-lh-surface py-16 px-8">
        <div className="max-w-6xl mx-auto">
          <h2 className="text-3xl font-black mb-6">Danh mục hàng đầu</h2>
          <div className="grid grid-cols-4 gap-4">
            {categories.map((cat) => (
              <button key={cat.name} onClick={() => navigate('/courses')} className="bg-white border border-lh-border rounded-2xl p-5 flex items-center gap-4 hover:shadow-lg hover:-translate-y-1 transition">
                <div className="w-14 h-14 rounded-full flex-none" style={{ backgroundColor: cat.color }}></div>
                <div className="text-left"><div className="text-base font-black">{cat.name}</div><div className="text-sm text-lh-muted">{cat.count} khoá học</div></div>
              </button>
            ))}
          </div>
        </div>
      </section>

      {/* Instructors */}
      <section className="max-w-6xl mx-auto px-8 py-16">
        <h2 className="text-3xl font-black mb-6">Giảng viên nổi bật</h2>
        <div className="grid grid-cols-4 gap-5">
          {instructors.map((ins) => (
            <div key={ins.name} className="bg-white border border-lh-border rounded-2xl p-6 text-center hover:shadow-lg hover:-translate-y-1 transition">
              <div className="w-24 h-24 rounded-full mx-auto mb-4 flex items-center justify-center text-white text-3xl font-black" style={{ backgroundColor: ins.color }}>
                {ins.initials}
              </div>
              <div className="text-lg font-black mb-1">{ins.name}</div>
              <div className="text-sm text-lh-muted mb-4">{ins.role}</div>
              <div className="flex justify-center gap-2 mb-4">
                {['in', 'f', '@'].map((s) => (
                  <span key={s} className="w-9 h-9 rounded-full bg-[#F4F5FB] text-lh-blue flex items-center justify-center font-black text-sm hover:bg-lh-blue hover:text-white cursor-pointer">
                    {s}
                  </span>
                ))}
              </div>
              <div className="flex justify-center gap-6 pt-4 border-t border-[#EFF1F7]">
                <div><div className="text-base font-black text-lh-navy">{ins.rating}</div><div className="text-xs text-lh-muted font-semibold">Đánh giá</div></div>
                <div><div className="text-base font-black text-lh-navy">{ins.students}</div><div className="text-xs text-lh-muted font-semibold">Học viên</div></div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Testimonials */}
      <section className="bg-lh-surface py-16 px-8">
        <div className="max-w-6xl mx-auto">
          <div className="text-center max-w-2xl mx-auto mb-9">
            <h2 className="text-3xl font-black mb-3">Học viên nói gì về LearnHub</h2>
            <p className="text-base text-lh-muted">Hàng nghìn học viên đã thay đổi sự nghiệp nhờ các khoá học tại đây.</p>
          </div>
          <div className="grid grid-cols-3 gap-5">
            {testimonials.map((ts) => (
              <div key={ts.name} className="bg-white border border-lh-border rounded-2xl p-7 hover:shadow-lg hover:-translate-y-1 transition">
                <div className="text-[#F6A609] text-lg mb-3">★★★★★</div>
                <p className="text-sm text-lh-navy leading-relaxed mb-6">{ts.quote}</p>
                <div className="flex items-center gap-3 pt-4 border-t border-[#EFF1F7]">
                  <div className="w-12 h-12 rounded-full flex items-center justify-center text-white font-black text-sm flex-none" style={{ backgroundColor: ts.color }}>
                    {ts.initials}
                  </div>
                  <div><div className="text-sm font-black">{ts.name}</div><div className="text-xs text-lh-muted">{ts.role}</div></div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Band */}
      <section className="bg-lh-navy text-white py-20 px-8 text-center">
        <h2 className="text-4xl font-black mb-3">Sẵn sàng bắt đầu hành trình học tập?</h2>
        <p className="text-lg text-[#C9C8E8] max-w-2xl mx-auto mb-8">Tham gia cùng 73 triệu học viên đang nâng cao kỹ năng mỗi ngày trên LearnHub.</p>
        <button onClick={() => navigate('/signup')} className="h-14 px-9 bg-lh-pink text-white font-bold text-base rounded hover:bg-lh-pink-dark">
          Tạo tài khoản miễn phí
        </button>
      </section>
    </div>
  );
};

export default HomePage;
