/**
 * Tests for AssessmentSubmissionForm component
 *
 * Verifies:
 * - URL validation (GitHub format)
 * - Form submission
 * - Error display
 * - Loading state UI
 * - Input interaction
 */

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AssessmentSubmissionForm } from './assessment-submission-form';

describe('AssessmentSubmissionForm', () => {
  it('renders form with input and button', () => {
    const mockSubmit = jest.fn();
    render(<AssessmentSubmissionForm onSubmit={mockSubmit} />);

    expect(screen.getByLabelText(/GitHub Repository URL/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Submit for Assessment/i })).toBeInTheDocument();
  });

  it('validates empty URL', async () => {
    const mockSubmit = jest.fn();
    render(<AssessmentSubmissionForm onSubmit={mockSubmit} />);

    const button = screen.getByRole('button', { name: /Submit for Assessment/i });
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText(/Repository URL is required/i)).toBeInTheDocument();
    });

    expect(mockSubmit).not.toHaveBeenCalled();
  });

  it('validates invalid URL format', async () => {
    const mockSubmit = jest.fn();
    render(<AssessmentSubmissionForm onSubmit={mockSubmit} />);

    const input = screen.getByPlaceholderText(/https:\/\/github.com/i);
    fireEvent.change(input, { target: { value: 'not-a-url' } });

    const button = screen.getByRole('button', { name: /Submit for Assessment/i });
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText(/Please enter a valid GitHub repository URL/i)).toBeInTheDocument();
    });

    expect(mockSubmit).not.toHaveBeenCalled();
  });

  it('accepts valid GitHub URL', async () => {
    const mockSubmit = jest.fn().mockResolvedValue(undefined);
    render(<AssessmentSubmissionForm onSubmit={mockSubmit} />);

    const input = screen.getByPlaceholderText(/https:\/\/github.com/i);
    fireEvent.change(input, { target: { value: 'https://github.com/user/repo' } });

    const button = screen.getByRole('button', { name: /Submit for Assessment/i });
    fireEvent.click(button);

    await waitFor(() => {
      expect(mockSubmit).toHaveBeenCalledWith('https://github.com/user/repo');
    });
  });

  it('disables input and button while loading', () => {
    const mockSubmit = jest.fn();
    render(<AssessmentSubmissionForm onSubmit={mockSubmit} isLoading={true} />);

    const input = screen.getByPlaceholderText(/https:\/\/github.com/i) as HTMLInputElement;
    const button = screen.getByRole('button', { name: /Analyzing Repository/i });

    expect(input.disabled).toBe(true);
    expect(button.disabled).toBe(true);
    expect(screen.getByText(/Analyzing Repository/i)).toBeInTheDocument();
  });

  it('displays error message', () => {
    const mockSubmit = jest.fn();
    const error = new Error('Network failed');
    render(<AssessmentSubmissionForm onSubmit={mockSubmit} error={error} />);

    expect(screen.getByText(/Network failed/i)).toBeInTheDocument();
  });

  it('clears validation error on input change', async () => {
    const mockSubmit = jest.fn();
    render(<AssessmentSubmissionForm onSubmit={mockSubmit} />);

    const input = screen.getByPlaceholderText(/https:\/\/github.com/i);
    const button = screen.getByRole('button', { name: /Submit for Assessment/i });

    // Trigger validation error
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText(/Repository URL is required/i)).toBeInTheDocument();
    });

    // Change input should clear error
    fireEvent.change(input, { target: { value: 'https://github.com/user/repo' } });

    await waitFor(() => {
      expect(screen.queryByText(/Repository URL is required/i)).not.toBeInTheDocument();
    });
  });

  it('respects disabled prop', () => {
    const mockSubmit = jest.fn();
    render(<AssessmentSubmissionForm onSubmit={mockSubmit} disabled={true} />);

    const input = screen.getByPlaceholderText(/https:\/\/github.com/i) as HTMLInputElement;
    const button = screen.getByRole('button') as HTMLButtonElement;

    expect(input.disabled).toBe(true);
    expect(button.disabled).toBe(true);
  });
});
