import React, { useMemo } from 'react';
import { RadarSeriesDto } from '@/types/assessment';
import './GapAnalysisPanel.css';

interface GapAnalysisPanelProps {
  radarData: RadarSeriesDto;
  selectedAxis?: string;
}

export const GapAnalysisPanel: React.FC<GapAnalysisPanelProps> = ({
  radarData,
  selectedAxis
}) => {
  const activeSeries = useMemo(() => {
    return selectedAxis
      ? radarData.seriesList.find(s => s.name === selectedAxis)
      : radarData.seriesList[0];
  }, [selectedAxis, radarData]);

  if (!activeSeries) return null;

  return (
    <div className="gap-analysis-panel">
      <header className="gap-header">
        <h2>🎯 {activeSeries.name} Improvement Plan</h2>
        <span className={`level-badge level-${activeSeries.level.toLowerCase()}`}>
          {activeSeries.level}
        </span>
        <span className="score-display">{activeSeries.value}/100</span>
      </header>

      <section className="gap-analysis-text">
        <h3>Current Status</h3>
        <p className="gap-description">
          {activeSeries.gapAnalysis}
        </p>
      </section>

      <section className="quick-fix">
        <h3>💡 Quick Fix Template</h3>

        {activeSeries.name === 'Security' && (
          <QuickFixBlock
            title="SQL Injection Prevention"
            problem={`String query = "SELECT * FROM users WHERE email = '" + email + "'";`}
            fix={`// ❌ BEFORE: String concatenation (vulnerable)
String query = "SELECT * FROM users WHERE email = '" + email + "'";

// ✅ AFTER: Parameterized query (safe)
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmail(@Param("email") String email);`}
          />
        )}

        {activeSeries.name === 'Database' && (
          <QuickFixBlock
            title="Connection Pooling & Resource Management"
            problem={`Connection conn = DriverManager.getConnection(url);`}
            fix={`// ❌ BEFORE: Manual connection management
Connection conn = DriverManager.getConnection(url);
try {
  // use conn
} finally {
  conn.close();
}

// ✅ AFTER: Use Spring JdbcTemplate (pooling + auto-cleanup)
@Autowired
private JdbcTemplate jdbcTemplate;

public List<User> getUsers() {
  return jdbcTemplate.query("SELECT * FROM users",
    new UserRowMapper());
}`}
          />
        )}

        {activeSeries.name === 'Architecture' && (
          <QuickFixBlock
            title="Use DTOs for REST Endpoints"
            problem={`@RestController public class UserController { @GetMapping public List<User> getUsers() { return repo.findAll(); } }`}
            fix={`// ❌ BEFORE: Returning entities directly (tight coupling)
@RestController
public class UserController {
  @GetMapping
  public List<User> getUsers() {
    return userRepository.findAll();
  }
}

// ✅ AFTER: Use DTOs to decouple REST from persistence
@RestController
public class UserController {
  @GetMapping
  public List<UserDTO> getUsers() {
    return userRepository.findAll()
      .stream()
      .map(UserDTO::fromEntity)
      .collect(Collectors.toList());
  }
}`}
          />
        )}

        {activeSeries.name === 'Code Quality' && (
          <QuickFixBlock
            title="Safe Optional Handling"
            problem={`return userRepository.findById(id).get();`}
            fix={`// ❌ BEFORE: Unsafe Optional access (throws exception)
return userRepository.findById(id).get();

// ✅ AFTER: Safe handling with orElseThrow
return userRepository.findById(id)
  .orElseThrow(() -> new UserNotFoundException(id));

// OR: Graceful null return
return userRepository.findById(id)
  .map(this::transformToDTO)
  .orElse(null);`}
          />
        )}
      </section>

      <footer className="gap-footer">
        <p className="hint">💡 Click on another axis in the radar chart to see different improvement areas</p>
      </footer>
    </div>
  );
};

interface QuickFixBlockProps {
  title: string;
  problem: string;
  fix: string;
}

const QuickFixBlock: React.FC<QuickFixBlockProps> = ({ title, problem, fix }) => {
  const [copied, setCopied] = React.useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(fix).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  };

  return (
    <div className="quick-fix-block">
      <h4>{title}</h4>

      <div className="problem">
        <label>❌ Problem:</label>
        <code className="code-snippet problem-code">{problem}</code>
      </div>

      <div className="solution">
        <label>✅ Solution:</label>
        <pre className="code-snippet solution-code">
          <code>{fix}</code>
        </pre>
        <button onClick={handleCopy} className={`copy-btn ${copied ? 'copied' : ''}`}>
          {copied ? '✓ Copied!' : '📋 Copy Code'}
        </button>
      </div>
    </div>
  );
};
