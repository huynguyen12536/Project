import React, { useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { useLocation } from 'react-router-dom';
import { RouteSkeleton } from './RouteSkeleton';

interface PageTransitionProps {
  children: React.ReactNode;
  compact?: boolean;
}

export const PageTransition: React.FC<PageTransitionProps> = ({ children, compact = false }) => {
  const location = useLocation();
  const initialRender = useRef(true);
  const [showSkeleton, setShowSkeleton] = useState(false);

  useEffect(() => {
    if (initialRender.current) {
      initialRender.current = false;
      return undefined;
    }

    setShowSkeleton(true);
    const timer = window.setTimeout(() => setShowSkeleton(false), 220);
    return () => window.clearTimeout(timer);
  }, [location.pathname, location.search, location.hash]);

  const routeKey = `${location.pathname}${location.search}${location.hash}`;

  return (
    <div className={compact ? 'relative min-h-screen overflow-hidden' : 'relative min-h-[calc(100vh-66px)] overflow-hidden'}>
      <AnimatePresence mode="wait" initial={false}>
        <motion.div
          key={routeKey}
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -8 }}
          transition={{ duration: 0.22, ease: 'easeOut' }}
          className={compact ? 'min-h-screen will-change-transform' : 'min-h-[calc(100vh-66px)] will-change-transform'}
        >
          {children}
        </motion.div>
      </AnimatePresence>

      <AnimatePresence>
        {showSkeleton && (
          <motion.div
            key="route-skeleton"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.18 }}
            className="pointer-events-none absolute inset-0 z-20 bg-[rgba(247,248,252,0.92)] backdrop-blur-[2px]"
          >
            <RouteSkeleton path={location.pathname} compact={compact} />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default PageTransition;
