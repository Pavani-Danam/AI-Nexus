import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { FormInput, PasswordInput, FormButton, AlertBanner } from '../components/ui/FormControls';

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const from = location.state?.from?.pathname || '/dashboard';

  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });

  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [loading, setLoading] = useState(false);

  const validateForm = () => {
    const newErrors = {};

    if (!formData.email.trim()) {
      newErrors.email = 'Email address is required.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      newErrors.email = 'Please enter a valid email address.';
    }

    if (!formData.password) {
      newErrors.password = 'Password is required.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerError('');

    if (!validateForm()) {
      return;
    }

    setLoading(true);

    try {
      await login({
        email: formData.email.trim(),
        password: formData.password
      });

      // Navigate to destination or default to dashboard
      navigate(from, { replace: true });
    } catch (err) {
      if (err.response) {
        if (err.response.status === 401) {
          setServerError('Invalid email or password.');
        } else if (err.response.status === 400) {
          setServerError(err.response.data?.message || 'Invalid login details.');
        } else {
          setServerError(err.response.data?.message || 'Authentication failed. Please try again.');
        }
      } else {
        setServerError('Unable to connect to the server. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950 px-4 py-12 transition-colors duration-200">
      <div className="w-full max-w-md space-y-6">
        {/* Branding & Header */}
        <div className="text-center">
          <Link to="/" className="inline-flex items-center gap-2 mb-4">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-cyan-500 flex items-center justify-center font-bold text-white shadow-md shadow-indigo-500/20 text-lg">
              AI
            </div>
          </Link>
          <h2 className="text-2xl font-bold text-white tracking-tight">
            Sign in to AI-Nexus
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Access your enterprise workspaces and knowledge base
          </p>
        </div>

        {/* Form Card */}
        <div className="p-8 rounded-2xl bg-slate-900 border border-slate-800 shadow-sm space-y-5">
          <AlertBanner type="error" message={serverError} onClose={() => setServerError('')} />

          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <FormInput
              label="Email Address"
              id="email"
              type="email"
              placeholder="name@company.com"
              value={formData.email}
              onChange={handleChange}
              error={errors.email}
              disabled={loading}
              required
              autoComplete="email"
            />

            <PasswordInput
              label="Password"
              id="password"
              placeholder="••••••••"
              value={formData.password}
              onChange={handleChange}
              error={errors.password}
              disabled={loading}
              required
              autoComplete="current-password"
            />

            <div className="pt-2">
              <FormButton loading={loading} disabled={loading}>
                Sign In
              </FormButton>
            </div>
          </form>
        </div>

        {/* Footer Link */}
        <p className="text-center text-xs text-slate-400">
          Don't have an account?{' '}
          <Link to="/register" className="font-semibold text-indigo-400 hover:underline">
            Register
          </Link>
        </p>
      </div>
    </div>
  );
}
