import { useNavigate, useParams } from 'react-router-dom';

const CourseDetailPage = () => {
  const navigate = useNavigate();
  useParams();

  return (
    <div className="min-h-screen">
      {/* Dark Hero */}
      <section className="bg-lh-navy text-white py-12">
        <div className="max-w-6xl mx-auto px-8 grid grid-cols-2 gap-10">
          <div>
            <div className="text-xs text-[#99738E] font-semibold mb-3">
              <button onClick={() => navigate('/')} className="cursor-pointer hover:text-white">Trang chủ</button> ›
              <button onClick={() => navigate('/courses')} className="cursor-pointer hover:text-white"> Tất cả khoá học</button> ›
              <span className="text-[#C9C8E8]"> Lập trình</span>
            </div>
            <h1 className="text-4xl font-black leading-tight mb-3">Lập trình Web Full-Stack 2026</h1>
            <p className="text-lg text-[#C9C8E8] mb-4 max-w-lg">Xây dựng ứng dụng web hoàn chỉnh với React, Node.js, PostgreSQL và triển khai lên cloud — từ con số 0 đến sản phẩm thực tế.</p>
            <div className="flex items-center gap-4 flex-wrap mb-4">
              <span className="flex items-center gap-1"><span className="text-[#F6A609] font-bold">4.8</span><span className="text-[#F6A609]">★★★★★</span><span className="text-[#99738E]">(12.430 đánh giá)</span></span>
              <span className="text-[#C9C8E8]">73.200 học viên</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-[#C9C8E8]">
              <span className="w-8 h-8 rounded-full bg-lh-pink flex items-center justify-center text-white font-bold text-xs">TQ</span>
              Giảng viên: <strong className="text-white">Trần Minh Quân</strong>
            </div>
          </div>
          <div></div>
        </div>
      </section>

      {/* Content */}
      <div className="max-w-6xl mx-auto px-8 py-12 grid grid-cols-3 gap-10 items-start">
        {/* Left - Main Content */}
        <div className="col-span-2">
          {/* What You'll Learn */}
          <div className="border border-lh-border p-7 mb-8">
            <h3 className="text-2xl font-black mb-4">Bạn sẽ học được gì</h3>
            <div className="grid grid-cols-2 gap-5">
              {[
                'Xây dựng REST API với Node.js và Express',
                'Thành thạo React, hooks và quản lý state',
                'Thiết kế cơ sở dữ liệu với PostgreSQL',
                'Xác thực người dùng với JWT và OAuth',
                'Triển khai ứng dụng lên cloud (Vercel, Render)',
                'Quy trình làm việc Git và CI/CD cơ bản',
              ].map((item) => (
                <div key={item} className="flex gap-2 text-sm text-lh-navy leading-relaxed">
                  <span className="text-green-700 font-black flex-none">✓</span>
                  {item}
                </div>
              ))}
            </div>
          </div>

          {/* Curriculum */}
          <h3 className="text-2xl font-black mb-4">Nội dung khoá học</h3>
          <div className="border border-lh-border mb-8">
            {[
              { sign: '−', title: 'Giới thiệu & Cài đặt môi trường', meta: '5 bài · 42 phút', open: true, lessons: [{ title: 'Tổng quan khoá học', dur: '06:12' }, { title: 'Cài đặt Node, VS Code', dur: '11:40' }, { title: 'Cấu trúc dự án', dur: '09:25' }] },
              { sign: '+', title: 'Nền tảng React hiện đại', meta: '12 bài · 3h 10m', open: false, lessons: [] },
              { sign: '+', title: 'Xây dựng Backend với Node.js', meta: '14 bài · 4h 05m', open: false, lessons: [] },
              { sign: '+', title: 'Triển khai & Tối ưu', meta: '8 bài · 2h 18m', open: false, lessons: [] },
            ].map((sec) => (
              <div key={sec.title} className="border-b border-[#EFF1F7] last:border-b-0">
                <div className="flex items-center justify-between p-4 bg-[#F8F9FC] cursor-pointer hover:bg-[#F0F2FA]">
                  <div className="flex items-center gap-3">
                    <span className="text-2xl font-black text-lh-blue">{sec.sign}</span>
                    <span className="text-base font-black">{sec.title}</span>
                  </div>
                  <span className="text-sm text-lh-muted font-semibold">{sec.meta}</span>
                </div>
                {sec.open && (
                  <div className="p-4">
                    {sec.lessons.map((ls) => (
                      <div key={ls.title} className="flex items-center gap-3 py-2 text-sm text-lh-navy">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#2F2FA2" strokeWidth="1.8" className="flex-none">
                          <circle cx="12" cy="12" r="9" />
                          <path d="M10 9l5 3-5 3z" fill="#2F2FA2" />
                        </svg>
                        <span className="flex-1">{ls.title}</span>
                        <span className="text-lh-muted text-xs">{ls.dur}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>

          {/* Instructor */}
          <h3 className="text-2xl font-black mb-4">Giảng viên</h3>
          <div className="flex gap-5 pb-2">
            <div className="w-24 h-24 bg-lh-pink text-white flex items-center justify-center rounded-2xl text-4xl font-black flex-none">TQ</div>
            <div>
              <div className="text-2xl font-black mb-1">Trần Minh Quân</div>
              <div className="text-sm text-lh-muted mb-3">Senior Full-Stack Engineer · 8 năm kinh nghiệm</div>
              <p className="text-sm text-[#4A4E66] leading-relaxed max-w-2xl">Từng dẫn dắt đội ngũ phát triển tại các startup công nghệ, Quân đã đào tạo hơn 73.000 học viên với phương pháp thực chiến, tập trung vào dự án thật và best practices trong ngành.</p>
            </div>
          </div>
        </div>

        {/* Right - Sticky Purchase Card */}
        <div className="sticky top-24">
          <div className="bg-white border border-lh-border rounded-2xl shadow-lg overflow-hidden">
            {/* Video Thumbnail */}
            <div className="h-52 bg-[#553D67] flex items-center justify-center relative cursor-pointer hover:bg-[#6B4F7F]">
              <div className="w-16 h-16 bg-white/95 rounded-full flex items-center justify-center">
                <div className="w-0 h-0 border-t-5 border-b-5 border-l-8 border-t-transparent border-b-transparent border-l-lh-navy ml-1"></div>
              </div>
              <span className="absolute bottom-3 left-0 right-0 text-center text-white text-xs font-bold">Xem giới thiệu khoá học</span>
            </div>

            {/* Purchase Details */}
            <div className="p-6">
              <div className="flex items-center gap-2 mb-4">
                <span className="text-3xl font-black text-lh-navy tracking-tight">499.000₫</span>
                <span className="text-sm text-lh-muted line-through">899.000₫</span>
                <span className="bg-[#FDE7EC] text-lh-pink text-xs font-black px-2 py-1 ml-auto">-44%</span>
              </div>

              <button onClick={() => navigate('/')} className="w-full h-14 bg-lh-pink text-white font-bold text-base rounded-lg mb-3 hover:bg-lh-pink-dark">
                Mua ngay
              </button>

              <button onClick={() => navigate('/')} className="w-full h-14 bg-white text-lh-navy border border-lh-navy font-bold text-base rounded-lg mb-5 hover:bg-[#F4F5FB]">
                Thêm vào giỏ
              </button>

              <div className="text-xs font-black uppercase tracking-wider text-lh-muted mb-3">Khoá học bao gồm</div>
              <div className="flex flex-col gap-3">
                {[
                  '38 giờ video theo yêu cầu',
                  '64 bài tập thực hành & dự án',
                  'Truy cập trọn đời, mọi thiết bị',
                  'Chứng chỉ hoàn thành',
                  'Hỗ trợ hỏi đáp từ giảng viên',
                ].map((inc) => (
                  <div key={inc} className="flex gap-2 text-sm text-lh-navy">
                    <span className="text-lh-blue font-black flex-none">›</span>
                    {inc}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CourseDetailPage;
