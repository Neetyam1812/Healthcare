/**
 * FamilyCare - Core Theme, Mode & Accessibility Engine
 *
 * Copy this snippet into <head> before any CSS to prevent flash of wrong theme:
 * <script>
 *   (function() {
 *     try {
 *       var t = localStorage.getItem('familycare-theme');
 *       if (!t && window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) t = 'dark';
 *       if (t) document.documentElement.setAttribute('data-theme', t);
 *       if (localStorage.getItem('familycare-senior') === 'true') document.documentElement.classList.add('senior');
 *     } catch (e) {}
 *   })();
 * </script>
 */

(function () {
  'use strict';

  const THEME_KEY = 'familycare-theme';
  const SENIOR_KEY = 'familycare-senior';
  const LANG_KEY = 'familycare-lang';

  /* --------------------------------------------------------------------------
     1. Theme Switcher (data-theme-toggle)
     -------------------------------------------------------------------------- */
  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    const toggles = document.querySelectorAll('[data-theme-toggle]');
    toggles.forEach(btn => {
      const isDark = theme === 'dark';
      btn.setAttribute('aria-label', isDark ? 'Switch to light mode' : 'Switch to dark mode');
      btn.setAttribute('title', isDark ? 'Switch to light mode' : 'Switch to dark mode');
    });
  }

  function initTheme() {
    const saved = localStorage.getItem(THEME_KEY);
    const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    const initial = saved || (prefersDark ? 'dark' : 'light');
    applyTheme(initial);

    if (window.matchMedia) {
      window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', e => {
        if (!localStorage.getItem(THEME_KEY)) {
          applyTheme(e.matches ? 'dark' : 'light');
        }
      });
    }

    document.addEventListener('click', e => {
      const toggle = e.target.closest('[data-theme-toggle]');
      if (!toggle) return;
      const current = document.documentElement.getAttribute('data-theme') || 'light';
      const next = current === 'dark' ? 'light' : 'dark';
      localStorage.setItem(THEME_KEY, next);
      applyTheme(next);
    });
  }

  /* --------------------------------------------------------------------------
     2. Senior Mode Switcher (data-senior-toggle)
     -------------------------------------------------------------------------- */
  function applySenior(enabled) {
    if (enabled) {
      document.documentElement.classList.add('senior');
    } else {
      document.documentElement.classList.remove('senior');
    }
    const toggles = document.querySelectorAll('[data-senior-toggle]');
    toggles.forEach(btn => {
      btn.setAttribute('aria-pressed', enabled ? 'true' : 'false');
      const label = btn.querySelector('.senior-label');
      if (label) label.textContent = enabled ? 'Senior mode on' : 'Senior mode';
    });
  }

  function initSenior() {
    const saved = localStorage.getItem(SENIOR_KEY) === 'true';
    applySenior(saved);

    document.addEventListener('click', e => {
      const toggle = e.target.closest('[data-senior-toggle]');
      if (!toggle) return;
      const current = document.documentElement.classList.contains('senior');
      const next = !current;
      localStorage.setItem(SENIOR_KEY, String(next));
      applySenior(next);
    });
  }

  /* --------------------------------------------------------------------------
     3. Navbar Glass On Scroll
     -------------------------------------------------------------------------- */
  function initNavbarScroll() {
    const navbar = document.querySelector('.navbar');
    if (!navbar) return;

    function checkScroll() {
      if (window.scrollY > 20) {
        navbar.classList.add('glass');
      } else {
        navbar.classList.remove('glass');
      }
    }

    window.addEventListener('scroll', checkScroll, { passive: true });
    checkScroll();
  }

  /* --------------------------------------------------------------------------
     4. Mobile Hamburger Drawer
     -------------------------------------------------------------------------- */
  function initMobileMenu() {
    const hamburger = document.getElementById('hamburger-toggle');
    const drawer = document.getElementById('mobile-drawer');
    if (!hamburger || !drawer) return;

    function setOpen(open) {
      hamburger.setAttribute('aria-expanded', String(open));
      drawer.classList.toggle('is-open', open);
    }

    hamburger.addEventListener('click', e => {
      e.stopPropagation();
      const open = hamburger.getAttribute('aria-expanded') === 'true';
      setOpen(!open);
    });

    drawer.querySelectorAll('a').forEach(a => {
      a.addEventListener('click', () => setOpen(false));
    });

    document.addEventListener('keydown', e => {
      if (e.key === 'Escape' && drawer.classList.contains('is-open')) {
        setOpen(false);
        hamburger.focus();
      }
    });

    document.addEventListener('click', e => {
      if (drawer.classList.contains('is-open') && !drawer.contains(e.target) && !hamburger.contains(e.target)) {
        setOpen(false);
      }
    });
  }

  /* --------------------------------------------------------------------------
     5. Scroll Reveal Observer (.reveal -> .is-visible)
     -------------------------------------------------------------------------- */
  function initReveal() {
    const elements = document.querySelectorAll('.reveal');
    if (!elements.length) return;

    if (!('IntersectionObserver' in window)) {
      elements.forEach(el => el.classList.add('is-visible'));
      return;
    }

    const observer = new IntersectionObserver((entries, obs) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible');
          obs.unobserve(entry.target);
        }
      });
    }, {
      rootMargin: '0px 0px -50px 0px',
      threshold: 0.15
    });

    elements.forEach(el => observer.observe(el));
  }

  /* --------------------------------------------------------------------------
     6. Language Selector Sync
     -------------------------------------------------------------------------- */
  function initLanguage() {
    const selects = document.querySelectorAll('.lang-select');
    const saved = localStorage.getItem(LANG_KEY) || 'en';

    selects.forEach(select => {
      select.value = saved;
      select.addEventListener('change', e => {
        const val = e.target.value;
        localStorage.setItem(LANG_KEY, val);
        selects.forEach(s => { s.value = val; });
      });
    });
  }

  /* --------------------------------------------------------------------------
     Bootstrap All
     -------------------------------------------------------------------------- */
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      initTheme();
      initSenior();
      initNavbarScroll();
      initMobileMenu();
      initReveal();
      initLanguage();
    });
  } else {
    initTheme();
    initSenior();
    initNavbarScroll();
    initMobileMenu();
    initReveal();
    initLanguage();
  }
})();
