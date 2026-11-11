export default function ErrorBox({ message, onRetry }) {
  if (!message) return null
  return (
    <div className="error-box">
      <strong>Something went wrong</strong>
      <p>{message}</p>
      {onRetry && <button className="btn btn-sm" onClick={onRetry}>Retry</button>}
    </div>
  )
}
