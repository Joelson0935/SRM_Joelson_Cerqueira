import { NavLink, Navigate, Route, Routes } from 'react-router-dom'

import { ExchangeRatePage } from './pages/ExchangeRatePage/ExchangeRatePage'
import { OperatorPanel } from './pages/OperatorPanel/OperatorPanel'
import { TransactionsGrid } from './pages/TransactionsGrid/TransactionsGrid'
import styles from './App.module.css'

const NAV_ITEMS = [
  { to: '/', label: 'Operator Panel', end: true },
  { to: '/transactions', label: 'Transactions', end: false },
  { to: '/exchange-rates', label: 'Exchange Rates', end: false },
]

/**
 * App shell: cabeçalho com navegação entre as três telas e área principal
 * com as rotas. Estado de servidor é gerenciado pelo RTK Query (Provider em
 * main.tsx).
 */
function App() {
  return (
    <div className={styles.app}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <div className={styles.brand}>
            <span className={styles.brandMark}>SRM</span>
            <span className={styles.brandName}>Credit Engine</span>
          </div>
          <nav className={styles.nav}>
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  isActive ? `${styles.navLink} ${styles.navLinkActive}` : styles.navLink
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>
      </header>

      <main className={styles.main}>
        <Routes>
          <Route path="/" element={<OperatorPanel />} />
          <Route path="/transactions" element={<TransactionsGrid />} />
          <Route path="/exchange-rates" element={<ExchangeRatePage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
