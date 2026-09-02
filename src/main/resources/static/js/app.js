/**
 * ============================================================================
 * DEV TRACKER - TACTICAL COMMAND CENTER CLIENT SCRIPT (JS)
 * Theme Switching, Instant Search, URL Platform Auto-Detection, and GSAP Motion.
 * ============================================================================
 */

document.addEventListener('DOMContentLoaded', () => {
    // ------------------------------------------------------------------------
    // 1. Theme Switcher (Sync with localStorage & prefers-color-scheme)
    // ------------------------------------------------------------------------
    const root = document.documentElement;
    const themeToggleBtn = document.querySelector('#theme-toggle');

    const applyTheme = (isDark) => {
        root.classList.toggle('dark', isDark);
        localStorage.setItem('dev-tracker-theme', isDark ? 'dark' : 'light');
    };

    if (themeToggleBtn) {
        themeToggleBtn.addEventListener('click', () => {
            const currentlyDark = root.classList.contains('dark');
            applyTheme(!currentlyDark);
        });
    }

    // ------------------------------------------------------------------------
    // 2. Dashboard Metric Counter Ring Animations
    // ------------------------------------------------------------------------
    const counterCards = document.querySelectorAll('.counter-card');
    counterCards.forEach((card) => {
        const ring = card.querySelector('.progress-ring');
        if (!ring) return;

        const count = Number(card.dataset.count || 0);
        const radius = ring.r.baseVal.value;
        const circumference = 2 * Math.PI * radius;

        ring.style.strokeDasharray = `${circumference}`;
        ring.style.strokeDashoffset = `${circumference}`;

        // Animate stroke dashoffset to target percentage (scaled to 100 max)
        const progressOffset = Math.max(circumference - (Math.min(count, 100) / 100) * circumference, 0);
        setTimeout(() => {
            ring.style.strokeDashoffset = `${progressOffset}`;
        }, 150);
    });

    // ------------------------------------------------------------------------
    // 3. Instant Live Search & Filter on Problem List
    // ------------------------------------------------------------------------
    const searchInput = document.querySelector('#problem-search');
    const problemCards = document.querySelectorAll('.problem-card');
    const emptyFilterNotice = document.querySelector('#no-filter-results');

    if (searchInput && problemCards.length) {
        searchInput.addEventListener('input', (e) => {
            const query = e.target.value.toLowerCase().trim();
            let visibleCount = 0;

            problemCards.forEach((card) => {
                const text = card.textContent.toLowerCase();
                const matches = text.includes(query);
                card.style.display = matches ? '' : 'none';
                if (matches) visibleCount++;
            });

            if (emptyFilterNotice) {
                emptyFilterNotice.classList.toggle('hidden', visibleCount > 0);
            }
        });
    }

    // ------------------------------------------------------------------------
    // 4. Global Keyboard Shortcuts (⌘K / Ctrl+K / '/' to Focus Search)
    // ------------------------------------------------------------------------
    document.addEventListener('keydown', (e) => {
        if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
            if (searchInput) {
                e.preventDefault();
                searchInput.focus();
            }
        } else if (e.key === '/' && document.activeElement.tagName !== 'INPUT' && document.activeElement.tagName !== 'TEXTAREA') {
            if (searchInput) {
                e.preventDefault();
                searchInput.focus();
            }
        }
    });

    // ------------------------------------------------------------------------
    // 5. Intelligent Problem URL Auto-Detection (Add Problem Form)
    // ------------------------------------------------------------------------
    const problemLinkInput = document.querySelector('#problemLink');
    const platformSelect = document.querySelector('#platform');
    const problemNameInput = document.querySelector('#problemName');

    if (problemLinkInput && platformSelect) {
        problemLinkInput.addEventListener('input', () => {
            const url = problemLinkInput.value.trim().toLowerCase();
            if (!url) return;

            // Auto-detect Platform
            let detectedPlatform = '';
            if (url.includes('leetcode.com')) detectedPlatform = 'LeetCode';
            else if (url.includes('geeksforgeeks.org')) detectedPlatform = 'GeeksforGeeks';
            else if (url.includes('codeforces.com')) detectedPlatform = 'Codeforces';
            else if (url.includes('codechef.com')) detectedPlatform = 'CodeChef';
            else if (url.includes('hackerrank.com')) detectedPlatform = 'HackerRank';

            if (detectedPlatform) {
                for (let option of platformSelect.options) {
                    if (option.text.toLowerCase().includes(detectedPlatform.toLowerCase())) {
                        platformSelect.value = option.value;
                        break;
                    }
                }
            }

            // Auto-populate Problem Name if empty and it's a LeetCode URL
            if (problemNameInput && !problemNameInput.value && url.includes('leetcode.com/problems/')) {
                try {
                    const parts = url.split('/problems/')[1].split('/')[0].split('-');
                    const formatted = parts.map(p => p.charAt(0).toUpperCase() + p.slice(1)).join(' ');
                    if (formatted) problemNameInput.value = formatted;
                } catch (err) {}
            }
        });
    }

    // ------------------------------------------------------------------------
    // 6. GSAP Motion & Micro-Interactions (Safe for prefers-reduced-motion)
    // ------------------------------------------------------------------------
    if (!window.gsap || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        return;
    }

    // Stagger in cards and header links
    gsap.from('.problem-card', {
        opacity: 0,
        y: 12,
        duration: 0.35,
        stagger: 0.04,
        ease: 'power2.out',
    });

    // Subtle scroll reveal
    const reveals = document.querySelectorAll('.reveal');
    if ('IntersectionObserver' in window) {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    gsap.to(entry.target, { opacity: 1, y: 0, duration: 0.5, ease: 'power2.out' });
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.1 });

        reveals.forEach((el) => observer.observe(el));
    }
});
