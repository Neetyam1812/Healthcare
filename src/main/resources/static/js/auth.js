/**
 * FamilyCare - Authentication Client Script (CALM CLINICAL – GLASS EDITION)
 * Validation, Password Strength Meter, Visibility Toggle, OTP Navigation & Step Flow
 */

(function () {
  'use strict';

  /* --------------------------------------------------------------------------
     1. Password Visibility Toggle
     -------------------------------------------------------------------------- */
  function initPasswordToggles() {
    const toggles = document.querySelectorAll('.auth-pw-toggle, .auth-eye-btn');
    toggles.forEach(btn => {
      btn.addEventListener('click', function () {
        const targetId = this.getAttribute('data-target');
        const input = document.getElementById(targetId);
        if (!input) return;

        const isPassword = input.type === 'password';
        input.type = isPassword ? 'text' : 'password';
        this.setAttribute('aria-pressed', isPassword ? 'true' : 'false');
        this.setAttribute('aria-label', isPassword ? 'Hide password' : 'Show password');

        const eyeOpen = this.querySelector('.icon-eye-open');
        const eyeClosed = this.querySelector('.icon-eye-closed');
        if (eyeOpen && eyeClosed) {
          eyeOpen.style.display = isPassword ? 'none' : 'block';
          eyeClosed.style.display = isPassword ? 'block' : 'none';
        }
      });
    });
  }

  /* --------------------------------------------------------------------------
     2. Email Validation Helper
     -------------------------------------------------------------------------- */
  function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
  }

  function setFieldError(fieldId, errorId, hasError, customMessage) {
    const input = document.getElementById(fieldId);
    const errorEl = document.getElementById(errorId);
    if (!input || !errorEl) return;

    if (hasError) {
      input.classList.add('has-error');
      errorEl.classList.add('is-visible');
      if (customMessage) {
        const textSpan = errorEl.querySelector('.error-text') || errorEl;
        textSpan.textContent = customMessage;
      }
    } else {
      input.classList.remove('has-error');
      errorEl.classList.remove('is-visible');
    }
  }

  /* --------------------------------------------------------------------------
     3. Password Strength Meter & Requirement Indicators
     -------------------------------------------------------------------------- */
  function initPasswordStrength() {
    const passwordInput = document.getElementById('register-password') || document.getElementById('new-password');
    if (!passwordInput) return;

    const reqLength = document.getElementById('req-length');
    const reqUpper = document.getElementById('req-uppercase');
    const reqNumber = document.getElementById('req-number');
    const strengthLabel = document.getElementById('strength-text');
    const bars = document.querySelectorAll('.pw-bar-segment, .pw-strength-bar');

    passwordInput.addEventListener('input', function () {
      const val = this.value;
      const hasLength = val.length >= 8;
      const hasUpper = /[A-Z]/.test(val);
      const hasNumber = /[0-9]/.test(val);
      const hasSpecial = /[^A-Za-z0-9]/.test(val);

      // Update Requirement Checkmarks
      if (reqLength) reqLength.classList.toggle('is-valid', hasLength);
      if (reqUpper) reqUpper.classList.toggle('is-valid', hasUpper);
      if (reqNumber) reqNumber.classList.toggle('is-valid', hasNumber);

      // Calculate Score
      let score = 0;
      if (hasLength) score++;
      if (hasUpper) score++;
      if (hasNumber) score++;
      if (hasSpecial || val.length >= 12) score++;

      // Update Bars
      bars.forEach((bar, idx) => {
        bar.className = bar.classList.contains('pw-bar-segment') ? 'pw-bar-segment' : 'pw-strength-bar';
        if (val.length > 0 && idx < score) {
          bar.classList.add('is-active');
          if (score === 1) bar.classList.add('strength-weak');
          else if (score === 2) bar.classList.add('strength-fair');
          else if (score === 3) bar.classList.add('strength-good');
          else if (score >= 4) bar.classList.add('strength-strong');
        }
      });

      // Update Label
      if (strengthLabel) {
        if (val.length === 0) strengthLabel.textContent = '';
        else if (score === 1) strengthLabel.textContent = 'Weak';
        else if (score === 2) strengthLabel.textContent = 'Fair';
        else if (score === 3) strengthLabel.textContent = 'Good';
        else if (score >= 4) strengthLabel.textContent = 'Strong';
      }
    });
  }

  /* --------------------------------------------------------------------------
     4. Login Page Form Handling
     -------------------------------------------------------------------------- */
  function initLoginForm() {
    const form = document.getElementById('login-form');
    if (!form) return;

    const emailInput = document.getElementById('login-email');
    const passwordInput = document.getElementById('login-password');
    const submitBtn = document.getElementById('login-submit-btn');

    emailInput.addEventListener('blur', function () {
      if (this.value && !isValidEmail(this.value)) {
        setFieldError('login-email', 'login-email-error', true, 'Please enter a valid email address.');
      } else {
        setFieldError('login-email', 'login-email-error', false);
      }
    });

    passwordInput.addEventListener('input', function () {
      if (this.value) {
        setFieldError('login-password', 'login-password-error', false);
      }
    });

    form.addEventListener('submit', function (e) {
      let isValid = true;

      if (!isValidEmail(emailInput.value)) {
        setFieldError('login-email', 'login-email-error', true, 'Please enter a valid email address.');
        isValid = false;
      } else {
        setFieldError('login-email', 'login-email-error', false);
      }

      if (!passwordInput.value.trim()) {
        setFieldError('login-password', 'login-password-error', true, 'Password is required.');
        isValid = false;
      } else {
        setFieldError('login-password', 'login-password-error', false);
      }

      if (!isValid) {
        e.preventDefault();
        return;
      }

      // Enter loading state
      if (submitBtn) {
        submitBtn.classList.add('is-loading');
        submitBtn.disabled = true;
      }
    });
  }

  /* --------------------------------------------------------------------------
     5. Registration Page Form Handling
     -------------------------------------------------------------------------- */
  function initRegisterForm() {
    const form = document.getElementById('register-form');
    if (!form) return;

    const nameInput = document.getElementById('register-name');
    const emailInput = document.getElementById('register-email');
    const passwordInput = document.getElementById('register-password');
    const confirmInput = document.getElementById('register-confirm');
    const termsCheckbox = document.getElementById('register-terms');
    const submitBtn = document.getElementById('register-submit-btn');

    emailInput.addEventListener('blur', function () {
      if (this.value && !isValidEmail(this.value)) {
        setFieldError('register-email', 'register-email-error', true, 'Please enter a valid email address.');
      } else {
        setFieldError('register-email', 'register-email-error', false);
      }
    });

    confirmInput.addEventListener('input', function () {
      if (this.value && this.value !== passwordInput.value) {
        setFieldError('register-confirm', 'register-confirm-error', true, 'Passwords do not match.');
      } else {
        setFieldError('register-confirm', 'register-confirm-error', false);
      }
    });

    form.addEventListener('submit', function (e) {
      let isValid = true;

      if (!nameInput.value.trim()) {
        setFieldError('register-name', 'register-name-error', true, 'Full name is required.');
        isValid = false;
      } else {
        setFieldError('register-name', 'register-name-error', false);
      }

      if (!isValidEmail(emailInput.value)) {
        setFieldError('register-email', 'register-email-error', true, 'Please enter a valid email address.');
        isValid = false;
      } else {
        setFieldError('register-email', 'register-email-error', false);
      }

      const pw = passwordInput.value;
      const isPwStrong = pw.length >= 8 && /[A-Z]/.test(pw) && /[0-9]/.test(pw);
      if (!isPwStrong) {
        setFieldError('register-password', 'register-password-error', true, 'Password must fulfill all requirements below.');
        isValid = false;
      } else {
        setFieldError('register-password', 'register-password-error', false);
      }

      if (confirmInput.value !== pw) {
        setFieldError('register-confirm', 'register-confirm-error', true, 'Passwords do not match.');
        isValid = false;
      } else {
        setFieldError('register-confirm', 'register-confirm-error', false);
      }

      if (!termsCheckbox.checked) {
        const termsError = document.getElementById('register-terms-error');
        if (termsError) termsError.classList.add('is-visible');
        isValid = false;
      } else {
        const termsError = document.getElementById('register-terms-error');
        if (termsError) termsError.classList.remove('is-visible');
      }

      if (!isValid) {
        e.preventDefault();
        return;
      }

      // Enter loading state
      if (submitBtn) {
        submitBtn.classList.add('is-loading');
        submitBtn.disabled = true;
      }
    });
  }

  /* --------------------------------------------------------------------------
     6. Six-Digit OTP Auto-Navigation & Paste Support
     -------------------------------------------------------------------------- */
  function initOtpInputs() {
    const otpBoxes = document.querySelectorAll('.modern-otp-box, .otp-box');
    if (!otpBoxes.length) return;

    otpBoxes.forEach((box, index) => {
      // Auto move forward on input
      box.addEventListener('input', function (e) {
        const val = this.value.replace(/[^0-9]/g, '');
        this.value = val ? val[val.length - 1] : '';

        if (this.value && index < otpBoxes.length - 1) {
          otpBoxes[index + 1].focus();
        }
      });

      // Backspace moves to previous input
      box.addEventListener('keydown', function (e) {
        if (e.key === 'Backspace' && !this.value && index > 0) {
          otpBoxes[index - 1].focus();
        }
      });

      // Paste support for 6 digits
      box.addEventListener('paste', function (e) {
        e.preventDefault();
        const pasteData = (e.clipboardData || window.clipboardData).getData('text').replace(/[^0-9]/g, '');
        if (!pasteData) return;

        const chars = pasteData.slice(0, otpBoxes.length).split('');
        chars.forEach((char, i) => {
          if (otpBoxes[i]) otpBoxes[i].value = char;
        });

        const nextIndex = Math.min(chars.length, otpBoxes.length - 1);
        otpBoxes[nextIndex].focus();
      });
    });
  }

  function getCsrfInfo() {
    const metaToken = document.querySelector('meta[name="_csrf"]');
    const metaHeader = document.querySelector('meta[name="_csrf_header"]');
    const token = metaToken ? metaToken.getAttribute('content') : '';
    const header = metaHeader ? metaHeader.getAttribute('content') : 'X-CSRF-TOKEN';
    return { token, header };
  }

  /* --------------------------------------------------------------------------
     7. Forgot Password 3-Step Flow
     -------------------------------------------------------------------------- */
  function initForgotPasswordFlow() {
    const pageContainer = document.getElementById('forgot-password-page');
    if (!pageContainer) return;

    let resendInterval = null;
    let countdownSec = 60;

    function goToStep(stepNumber) {
      document.querySelectorAll('.auth-step-container').forEach(el => el.classList.remove('is-active'));
      const targetStep = document.getElementById('step-' + stepNumber);
      if (targetStep) targetStep.classList.add('is-active');

      // Update Top Nav Indicator
      const pills = document.querySelectorAll('.stepper-node, .auth-step-pill');
      pills.forEach((pill, idx) => {
        const pillStep = idx + 1;
        pill.classList.remove('is-active', 'is-completed');
        if (stepNumber === 'success' || pillStep < stepNumber) pill.classList.add('is-completed');
        else if (pillStep === stepNumber) pill.classList.add('is-active');
      });
    }

    function startResendCountdown() {
      const resendBtn = document.getElementById('resend-otp-btn');
      const timerSpan = document.getElementById('resend-timer');
      if (!resendBtn || !timerSpan) return;

      countdownSec = 60;
      resendBtn.disabled = true;
      if (resendInterval) clearInterval(resendInterval);

      resendInterval = setInterval(() => {
        countdownSec--;
        const mins = String(Math.floor(countdownSec / 60)).padStart(2, '0');
        const secs = String(countdownSec % 60).padStart(2, '0');
        timerSpan.textContent = `(${mins}:${secs})`;

        if (countdownSec <= 0) {
          clearInterval(resendInterval);
          resendBtn.disabled = false;
          timerSpan.textContent = '';
        }
      }, 1000);
    }

    // STEP 1: Enter Email
    const step1Btn = document.getElementById('fp-step1-btn');
    const fpEmail = document.getElementById('fp-email');
    if (step1Btn && fpEmail) {
      step1Btn.addEventListener('click', function () {
        if (!isValidEmail(fpEmail.value)) {
          setFieldError('fp-email', 'fp-email-error', true, 'Please enter a valid registered email address.');
          return;
        }
        setFieldError('fp-email', 'fp-email-error', false);

        step1Btn.classList.add('is-loading');
        step1Btn.disabled = true;

        const csrf = getCsrfInfo();
        const formData = new URLSearchParams();
        formData.append('email', fpEmail.value.trim());

        const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
        if (csrf.token) headers[csrf.header] = csrf.token;

        fetch('/forgot-password/send-otp', {
          method: 'POST',
          headers: headers,
          body: formData.toString()
        })
        .then(res => res.json())
        .then(data => {
          step1Btn.classList.remove('is-loading');
          step1Btn.disabled = false;

          const displayEmail = document.getElementById('user-email-preview');
          if (displayEmail) displayEmail.textContent = fpEmail.value.trim();

          goToStep(2);
          startResendCountdown();

          const firstOtp = document.querySelector('.modern-otp-box, .otp-box');
          if (firstOtp) firstOtp.focus();
        })
        .catch(err => {
          step1Btn.classList.remove('is-loading');
          step1Btn.disabled = false;

          const displayEmail = document.getElementById('user-email-preview');
          if (displayEmail) displayEmail.textContent = fpEmail.value.trim();

          goToStep(2);
          startResendCountdown();
        });
      });
    }

    // Change Email Link in Step 2
    const changeEmailBtn = document.getElementById('change-email-btn');
    if (changeEmailBtn) {
      changeEmailBtn.addEventListener('click', function (e) {
        e.preventDefault();
        goToStep(1);
      });
    }

    // Resend OTP Button in Step 2
    const resendBtn = document.getElementById('resend-otp-btn');
    if (resendBtn) {
      resendBtn.addEventListener('click', function () {
        if (this.disabled) return;
        const email = (document.getElementById('user-email-preview')?.textContent || fpEmail?.value || '').trim();
        if (!email) return;

        const csrf = getCsrfInfo();
        const formData = new URLSearchParams();
        formData.append('email', email);

        const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
        if (csrf.token) headers[csrf.header] = csrf.token;

        fetch('/forgot-password/send-otp', {
          method: 'POST',
          headers: headers,
          body: formData.toString()
        }).finally(() => {
          startResendCountdown();
        });
      });
    }

    // STEP 2: Verify OTP
    const step2Btn = document.getElementById('fp-step2-btn');
    if (step2Btn) {
      step2Btn.addEventListener('click', function () {
        const otpBoxes = document.querySelectorAll('.modern-otp-box, .otp-box');
        let code = '';
        otpBoxes.forEach(b => { code += b.value; });

        if (code.length < 6) {
          const otpError = document.getElementById('otp-error-msg');
          if (otpError) {
            const span = otpError.querySelector('span:last-child') || otpError;
            span.textContent = 'Please enter all 6 digits of your verification code.';
            otpError.classList.add('is-visible');
          }
          return;
        }

        const otpError = document.getElementById('otp-error-msg');
        if (otpError) otpError.classList.remove('is-visible');

        step2Btn.classList.add('is-loading');
        step2Btn.disabled = true;

        const csrf = getCsrfInfo();
        const formData = new URLSearchParams();
        formData.append('otp', code);

        const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
        if (csrf.token) headers[csrf.header] = csrf.token;

        fetch('/forgot-password/verify-otp', {
          method: 'POST',
          headers: headers,
          body: formData.toString()
        })
        .then(res => res.json())
        .then(data => {
          step2Btn.classList.remove('is-loading');
          step2Btn.disabled = false;

          if (data.success) {
            goToStep(3);
            const newPw = document.getElementById('new-password');
            if (newPw) newPw.focus();
          } else {
            if (otpError) {
              const span = otpError.querySelector('span:last-child') || otpError;
              span.textContent = data.message || 'Invalid verification code.';
              otpError.classList.add('is-visible');
            }
          }
        })
        .catch(err => {
          step2Btn.classList.remove('is-loading');
          step2Btn.disabled = false;
          if (otpError) {
            const span = otpError.querySelector('span:last-child') || otpError;
            span.textContent = 'Verification error. Please try again.';
            otpError.classList.add('is-visible');
          }
        });
      });
    }

    // STEP 3: Create New Password
    const step3Btn = document.getElementById('fp-step3-btn');
    const newPassword = document.getElementById('new-password');
    const confirmNewPassword = document.getElementById('confirm-new-password');

    if (step3Btn && newPassword && confirmNewPassword) {
      step3Btn.addEventListener('click', function () {
        const pw = newPassword.value;
        const isPwStrong = pw.length >= 8 && /[A-Z]/.test(pw) && /[0-9]/.test(pw);

        if (!isPwStrong) {
          setFieldError('new-password', 'new-password-error', true, 'Password must fulfill all requirements below.');
          return;
        } else {
          setFieldError('new-password', 'new-password-error', false);
        }

        if (confirmNewPassword.value !== pw) {
          setFieldError('confirm-new-password', 'confirm-new-error', true, 'Passwords do not match.');
          return;
        } else {
          setFieldError('confirm-new-password', 'confirm-new-error', false);
        }

        step3Btn.classList.add('is-loading');
        step3Btn.disabled = true;

        const csrf = getCsrfInfo();
        const formData = new URLSearchParams();
        formData.append('newPassword', pw);
        formData.append('confirmPassword', confirmNewPassword.value);

        const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
        if (csrf.token) headers[csrf.header] = csrf.token;

        fetch('/forgot-password/reset-password', {
          method: 'POST',
          headers: headers,
          body: formData.toString()
        })
        .then(res => res.json())
        .then(data => {
          step3Btn.classList.remove('is-loading');
          step3Btn.disabled = false;

          if (data.success) {
            goToStep('success');
          } else {
            setFieldError('new-password', 'new-password-error', true, data.message || 'Password reset failed.');
          }
        })
        .catch(err => {
          step3Btn.classList.remove('is-loading');
          step3Btn.disabled = false;
          setFieldError('new-password', 'new-password-error', true, 'Password reset failed. Please try again.');
        });
      });
    }
  }

  /* --------------------------------------------------------------------------
     Bootstrap Auth Handlers
     -------------------------------------------------------------------------- */
  document.addEventListener('DOMContentLoaded', function () {
    initPasswordToggles();
    initPasswordStrength();
    initLoginForm();
    initRegisterForm();
    initOtpInputs();
    initForgotPasswordFlow();
  });
})();
