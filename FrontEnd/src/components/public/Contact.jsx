import React, { useState } from "react";
import { useToast } from "../../context/ToastContext";

export default function Contact() {
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    message: "",
  });
  const [loading, setLoading] = useState(false);
  const { showSuccess, showWarning } = useToast();

  const handleChange = (e) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!formData.name.trim() || !formData.email.trim() || !formData.message.trim()) {
      showWarning("Please fill in all fields before submitting");
      return;
    }
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      showSuccess("Thank you! Your inquiry has been sent to our support team.");
      setFormData({ name: "", email: "", message: "" });
    }, 600);
  };

  return (
    <section className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-12 space-y-8">
      <div className="text-center space-y-1">
        <h1 className="text-3xl font-black text-slate-900 tracking-tight">
          Get in Touch with <span className="text-orange-500">SkillCheckr</span>
        </h1>
        <p className="text-sm text-slate-500">
          Have questions about institutional licensing, custom testing, or platform support?
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-start">
        {/* Contact Form */}
        <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 shadow-sm space-y-4">
          <h2 className="text-lg font-bold text-slate-800">Send us a Message</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Full Name
              </label>
              <input
                type="text"
                name="name"
                value={formData.name}
                onChange={handleChange}
                placeholder="Enter your name"
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:border-orange-500 focus:bg-white transition-all"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Email Address
              </label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="name@institution.edu"
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:border-orange-500 focus:bg-white transition-all"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Message
              </label>
              <textarea
                rows="4"
                name="message"
                value={formData.message}
                onChange={handleChange}
                placeholder="How can we assist your institution or department?"
                className="w-full p-4 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:border-orange-500 focus:bg-white transition-all resize-none"
                required
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 px-4 bg-slate-900 hover:bg-slate-800 text-white font-semibold rounded-xl shadow-md transition-all text-sm"
            >
              {loading ? "Sending..." : "Submit Inquiry"}
            </button>
          </form>
        </div>

        {/* Info & Map Frame */}
        <div className="space-y-6">
          <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-3">
            <h3 className="font-bold text-slate-900 text-sm">Academic Support Desk</h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              Our technical engineering and pedagogical support team is available Monday through Friday, 9:00 AM – 6:00 PM IST.
            </p>
            <div className="text-xs text-slate-700 space-y-1 pt-2">
              <div><strong>Email:</strong> support@skillcheckr.edu</div>
              <div><strong>Support Hotline:</strong> +91 98343 03107</div>
            </div>
          </div>

          <div className="rounded-3xl overflow-hidden border border-slate-200 shadow-sm h-64">
            <iframe
              src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3784.0665343041205!2d73.8028981!3d18.480645199999998!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x3bc2bfeb853d4691%3A0x56f1a2e46627167!2sGiri's%20TECH%20HUB%20Pvt%20Ltd!5e0!3m2!1sen!2sin!4v1742846485204!5m2!1sen!2sin"
              className="w-full h-full border-0"
              allowFullScreen=""
              loading="lazy"
              referrerPolicy="no-referrer-when-downgrade"
              title="Location Map"
            ></iframe>
          </div>
        </div>
      </div>
    </section>
  );
}
