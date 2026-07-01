import React, { useEffect, useState } from 'react';
import { Check, CirclePlus, GripVertical, HelpCircle, Trash2 } from 'lucide-react';
import { motion } from 'framer-motion';
import { Button, Input } from '../ui-kit';
import { cn } from '../lib/cn';

export interface QuizAnswer {
  id: string;
  text: string;
  isCorrect: boolean;
}

export interface QuizQuestion {
  id: string;
  text: string;
  answers: QuizAnswer[];
}

interface QuizBuilderProps {
  value?: QuizQuestion[];
  onChange?: (questions: QuizQuestion[]) => void;
  disabled?: boolean;
  className?: string;
}

const buildQuestion = (): QuizQuestion => ({
  id: `q-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
  text: '',
  answers: [
    { id: `a-${Date.now()}-1`, text: '', isCorrect: true },
    { id: `a-${Date.now()}-2`, text: '', isCorrect: false },
  ],
});

export const QuizBuilder: React.FC<QuizBuilderProps> = ({
  value = [],
  onChange,
  disabled = false,
  className,
}) => {
  const [questions, setQuestions] = useState<QuizQuestion[]>(value);

  useEffect(() => {
    setQuestions(value);
  }, [value]);

  const commit = (next: QuizQuestion[]) => {
    setQuestions(next);
    onChange?.(next);
  };

  const addQuestion = () => {
    commit([...questions, buildQuestion()]);
  };

  const updateQuestion = (questionId: string, updater: (question: QuizQuestion) => QuizQuestion) => {
    commit(questions.map((question) => (question.id === questionId ? updater(question) : question)));
  };

  const removeQuestion = (questionId: string) => {
    commit(questions.filter((question) => question.id !== questionId));
  };

  return (
    <div className={cn('space-y-4', className)}>
      <div className="flex items-center justify-between rounded-2xl border border-gray-200 bg-gray-50 px-4 py-3">
        <div>
          <p className="text-sm font-semibold text-gray-900">Cau hoi kiem tra</p>
          <p className="text-xs text-gray-500">
            Tao quiz ngan gon de hoc vien tu danh gia sau moi bai hoc.
          </p>
        </div>
        <Button type="button" variant="secondary" size="sm" onClick={addQuestion} disabled={disabled}>
          <CirclePlus className="h-4 w-4" />
          Them cau hoi
        </Button>
      </div>

      {questions.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-gray-300 bg-white px-6 py-12 text-center">
          <HelpCircle className="mx-auto h-10 w-10 text-gray-300" />
          <p className="mt-3 text-sm font-medium text-gray-700">Chua co cau hoi nao</p>
          <p className="mt-1 text-sm text-gray-500">Bat dau bang 1 cau hoi trac nghiem don gian.</p>
        </div>
      ) : null}

      {questions.map((question, questionIndex) => (
        <motion.div
          key={question.id}
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="rounded-3xl border border-gray-200 bg-white p-5 shadow-sm"
        >
          <div className="flex items-start justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="rounded-2xl bg-primary-50 p-2 text-primary-700">
                <GripVertical className="h-4 w-4" />
              </div>
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-gray-400">
                  Cau hoi {questionIndex + 1}
                </p>
                <p className="text-sm text-gray-500">Chon 1 dap an dung.</p>
              </div>
            </div>
            <button
              type="button"
              disabled={disabled}
              onClick={() => removeQuestion(question.id)}
              className="rounded-xl p-2 text-gray-400 transition hover:bg-red-50 hover:text-red-600 disabled:cursor-not-allowed"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </div>

          <div className="mt-4">
            <Input
              label="Noi dung cau hoi"
              value={question.text}
              disabled={disabled}
              onChange={(event) =>
                updateQuestion(question.id, (current) => ({ ...current, text: event.target.value }))
              }
              placeholder="Vi du: HTTP status nao bieu thi thanh cong?"
            />
          </div>

          <div className="mt-4 space-y-3">
            {question.answers.map((answer, answerIndex) => (
              <div
                key={answer.id}
                className={cn(
                  'flex items-center gap-3 rounded-2xl border px-4 py-3 transition',
                  answer.isCorrect ? 'border-emerald-200 bg-emerald-50' : 'border-gray-200 bg-gray-50'
                )}
              >
                <button
                  type="button"
                  disabled={disabled}
                  onClick={() =>
                    updateQuestion(question.id, (current) => ({
                      ...current,
                      answers: current.answers.map((item) => ({
                        ...item,
                        isCorrect: item.id === answer.id,
                      })),
                    }))
                  }
                  className={cn(
                    'flex h-8 w-8 items-center justify-center rounded-full border transition',
                    answer.isCorrect
                      ? 'border-emerald-500 bg-emerald-500 text-white'
                      : 'border-gray-300 bg-white text-transparent hover:border-primary-400'
                  )}
                >
                  <Check className="h-4 w-4" />
                </button>
                <Input
                  value={answer.text}
                  disabled={disabled}
                  onChange={(event) =>
                    updateQuestion(question.id, (current) => ({
                      ...current,
                      answers: current.answers.map((item) =>
                        item.id === answer.id ? { ...item, text: event.target.value } : item
                      ),
                    }))
                  }
                  placeholder={`Dap an ${answerIndex + 1}`}
                />
                <button
                  type="button"
                  disabled={disabled || question.answers.length <= 2}
                  onClick={() =>
                    updateQuestion(question.id, (current) => ({
                      ...current,
                      answers: current.answers.filter((item) => item.id !== answer.id),
                    }))
                  }
                  className="rounded-xl p-2 text-gray-400 transition hover:bg-red-50 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}

            <button
              type="button"
              disabled={disabled}
              onClick={() =>
                updateQuestion(question.id, (current) => ({
                  ...current,
                  answers: [
                    ...current.answers,
                    {
                      id: `a-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
                      text: '',
                      isCorrect: false,
                    },
                  ],
                }))
              }
              className="inline-flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-medium text-primary-700 transition hover:bg-primary-50 disabled:cursor-not-allowed"
            >
              <CirclePlus className="h-4 w-4" />
              Them dap an
            </button>
          </div>
        </motion.div>
      ))}
    </div>
  );
};

export default QuizBuilder;
