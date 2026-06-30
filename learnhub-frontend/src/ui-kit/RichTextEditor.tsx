import React, { useEffect } from 'react';
import { useEditor, EditorContent, type Editor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { cn } from '../lib/cn';

export interface RichTextEditorProps {
  value?: string; // HTML
  onChange?: (html: string) => void;
  placeholder?: string;
  className?: string;
  editable?: boolean;
}

const ToolbarButton: React.FC<{
  active?: boolean;
  onClick: () => void;
  children: React.ReactNode;
  label: string;
}> = ({ active, onClick, children, label }) => (
  <button
    type="button"
    aria-label={label}
    aria-pressed={active}
    onMouseDown={(e) => e.preventDefault()}
    onClick={onClick}
    className={cn(
      'rounded px-2 py-1 text-sm transition-colors',
      active ? 'bg-primary-100 text-primary-700' : 'text-gray-600 hover:bg-gray-100'
    )}
  >
    {children}
  </button>
);

const Toolbar: React.FC<{ editor: Editor }> = ({ editor }) => (
  <div className="flex flex-wrap gap-1 border-b border-gray-200 p-1">
    <ToolbarButton
      label="Bold"
      active={editor.isActive('bold')}
      onClick={() => editor.chain().focus().toggleBold().run()}
    >
      <strong>B</strong>
    </ToolbarButton>
    <ToolbarButton
      label="Italic"
      active={editor.isActive('italic')}
      onClick={() => editor.chain().focus().toggleItalic().run()}
    >
      <em>I</em>
    </ToolbarButton>
    <ToolbarButton
      label="Heading"
      active={editor.isActive('heading', { level: 2 })}
      onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
    >
      H2
    </ToolbarButton>
    <ToolbarButton
      label="Bullet list"
      active={editor.isActive('bulletList')}
      onClick={() => editor.chain().focus().toggleBulletList().run()}
    >
      • List
    </ToolbarButton>
    <ToolbarButton
      label="Ordered list"
      active={editor.isActive('orderedList')}
      onClick={() => editor.chain().focus().toggleOrderedList().run()}
    >
      1. List
    </ToolbarButton>
    <ToolbarButton
      label="Code block"
      active={editor.isActive('codeBlock')}
      onClick={() => editor.chain().focus().toggleCodeBlock().run()}
    >
      {'</>'}
    </ToolbarButton>
  </div>
);

export const RichTextEditor: React.FC<RichTextEditorProps> = ({
  value = '',
  onChange,
  className,
  editable = true,
}) => {
  const editor = useEditor({
    extensions: [StarterKit],
    content: value,
    editable,
    onUpdate: ({ editor: ed }) => onChange?.(ed.getHTML()),
    editorProps: {
      attributes: {
        class: 'prose prose-sm max-w-none min-h-[120px] p-3 focus:outline-none',
      },
    },
  });

  // Keep external value in sync when it changes from outside.
  useEffect(() => {
    if (editor && value !== editor.getHTML()) {
      editor.commands.setContent(value, { emitUpdate: false });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  return (
    <div className={cn('rounded-lg border border-gray-300', className)}>
      {editor && editable && <Toolbar editor={editor} />}
      <EditorContent editor={editor} />
    </div>
  );
};

export default RichTextEditor;
