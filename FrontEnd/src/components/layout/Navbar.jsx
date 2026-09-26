import React, { useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import Logo from "../../assets/images/logo/logo.png";
import { useAuth } from "../../context/AuthContext";
import { Icon } from "../common/Icons";

export default function Navbar() {
  const { user, isAuthenticated, role, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate("/authentication");
  };

  const navLinks = [
    { path: "/", text: "Home" },
    { path: "/about", text: "About" },
    { path: "/blog", text: "Blog" },
    { path: "/contact", text: "Contact" },
  ];

  return (
    <header className="w-full h-24 sm:h-28 bg-white/95 backdrop-blur-md border-b border-slate-200 sticky top-0 z-50 transition-all shadow-sm flex items-center">
      <div className="w-full max-w-[1536px] mx-auto h-full px-4 sm:px-8 lg:px-12 flex items-center justify-between">
        <Link to="/" className="flex items-center group py-0">
          <img
            src={Logo}
            alt="SkillCheckr"
            className="h-14 sm:h-16 md:h-18 w-auto object-contain transition-transform group-hover:scale-105"
          />
        </Link>

        <nav className="hidden md:flex items-center gap-10 lg:gap-12 h-full">
          {navLinks.map((link) => (
            <NavLink
              key={link.path}
              to={link.path}
              className={({ isActive }) =>
                `text-base sm:text-lg font-semibold transition-all relative inline-flex items-center py-2 ${
                  isActive
                    ? "text-orange-600 after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-orange-500 after:rounded-full"
                    : "text-slate-600 hover:text-slate-900"
                }`
              }
            >
              {link.text}
            </NavLink>
          ))}
        </nav>

        <div className="hidden md:flex items-center gap-4">
          {isAuthenticated ? (
            <>
              <Link
                to={`/dashboard/${(role || "student").toLowerCase()}`}
                className="inline-flex items-center justify-center gap-2.5 px-6 py-3 bg-orange-50 hover:bg-orange-100 text-orange-600 border border-orange-200 rounded-2xl text-sm sm:text-base font-bold transition-all shadow-sm"
              >
                {user?.profileImage ? (
                  <img
                    src={user.profileImage}
                    alt={user.username}
                    className="w-6 h-6 rounded-full object-cover border border-orange-300"
                  />
                ) : (
                  <Icon name="user" className="w-5 h-5" />
                )}
                <span>Dashboard ({role})</span>
              </Link>
              <button
                onClick={handleLogout}
                className="inline-flex items-center justify-center gap-2 px-4 py-3 text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded-2xl text-sm font-semibold transition-all"
                title="Sign out"
              >
                <Icon name="logout" className="w-5 h-5" />
                <span className="text-xs font-semibold">Sign Out</span>
              </button>
            </>
          ) : (
            <Link
              to="/authentication"
              className="inline-flex items-center justify-center gap-2 px-7 py-3.5 bg-orange-500 hover:bg-orange-600 active:bg-orange-700 text-white rounded-2xl text-base font-bold shadow-md hover:shadow-lg transition-all"
            >
              Sign In
            </Link>
          )}
        </div>

        <div className="md:hidden flex items-center">
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="p-2.5 rounded-xl text-slate-700 hover:text-slate-900 hover:bg-slate-100 flex items-center justify-center"
            aria-label="Toggle navigation menu"
          >
            <Icon name={mobileMenuOpen ? "x" : "plus"} className="w-7 h-7" />
          </button>
        </div>
      </div>

      {mobileMenuOpen && (
        <div className="md:hidden absolute top-full left-0 w-full bg-white border-b border-slate-200 px-6 pt-3 pb-6 space-y-3 shadow-xl">
          {navLinks.map((link) => (
            <NavLink
              key={link.path}
              to={link.path}
              onClick={() => setMobileMenuOpen(false)}
              className={({ isActive }) =>
                `block px-4 py-3 rounded-xl text-lg font-semibold ${
                  isActive ? "bg-orange-50 text-orange-600" : "text-slate-700 hover:bg-slate-50"
                }`
              }
            >
              {link.text}
            </NavLink>
          ))}
          <div className="pt-3 border-t border-slate-100">
            {isAuthenticated ? (
              <div className="flex flex-col gap-3">
                <Link
                  to={`/dashboard/${(role || "student").toLowerCase()}`}
                  onClick={() => setMobileMenuOpen(false)}
                  className="w-full text-center px-4 py-3.5 bg-orange-500 text-white font-bold rounded-2xl shadow-sm text-base"
                >
                  Dashboard ({role})
                </Link>
                <button
                  onClick={() => {
                    setMobileMenuOpen(false);
                    handleLogout();
                  }}
                  className="w-full text-center px-4 py-3.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold rounded-2xl text-base"
                >
                  Sign Out
                </button>
              </div>
            ) : (
              <Link
                to="/authentication"
                onClick={() => setMobileMenuOpen(false)}
                className="block w-full text-center px-4 py-3.5 bg-orange-500 hover:bg-orange-600 text-white font-bold rounded-2xl shadow-sm text-base"
              >
                Sign In
              </Link>
            )}
          </div>
        </div>
      )}
    </header>
  );
}
