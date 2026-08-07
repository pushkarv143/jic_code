import { useEffect } from 'react';
import Box from '@mui/material/Box';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import CircularProgress from '@mui/material/CircularProgress';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import type { Book, BookCategory } from '@/types';
import type { BookPayload } from '@/api/libraryApi';

const schema = yup.object({
  title: yup.string().required('Title is required').max(200),
  author: yup.string().required('Author is required').max(150),
  isbn: yup.string().required('ISBN is required').max(20),
  categoryId: yup.number().typeError('Category is required').required('Category is required'),
  publisher: yup.string().max(150).optional(),
  totalCopies: yup
    .number()
    .typeError('Total copies is required')
    .required('Total copies is required')
    .min(1, 'Must be at least 1'),
  rackNumber: yup.string().max(30).optional(),
  price: yup.number().typeError('Enter a valid price').min(0).optional(),
});
type FormValues = yup.InferType<typeof schema>;

export interface BookFormDialogProps {
  open: boolean;
  editing: Book | null;
  categories: BookCategory[];
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: BookPayload) => void;
}

/** Add/edit dialog for a book (title, author, ISBN, category, copies, rack, price). */
export function BookFormDialog({ open, editing, categories, saving, onClose, onSubmit }: BookFormDialogProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: yupResolver(schema),
    defaultValues: {
      title: '',
      author: '',
      isbn: '',
      categoryId: undefined,
      publisher: '',
      totalCopies: 1,
      rackNumber: '',
      price: undefined,
    },
  });

  useEffect(() => {
    if (open) {
      reset({
        title: editing?.title ?? '',
        author: editing?.author ?? '',
        isbn: editing?.isbn ?? '',
        categoryId: editing?.categoryId,
        publisher: editing?.publisher ?? '',
        totalCopies: editing?.totalCopies ?? 1,
        rackNumber: editing?.rackNumber ?? '',
        price: editing?.price ?? undefined,
      });
    }
  }, [open, editing, reset]);

  const submit = (values: FormValues) => {
    onSubmit({
      title: values.title,
      author: values.author,
      isbn: values.isbn,
      categoryId: values.categoryId,
      publisher: values.publisher || undefined,
      totalCopies: values.totalCopies,
      rackNumber: values.rackNumber || undefined,
      price: values.price,
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{editing ? 'Edit Book' : 'Add Book'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12} sm={8}>
              <TextField
                label="Title"
                fullWidth
                autoFocus
                {...register('title')}
                error={!!errors.title}
                helperText={errors.title?.message}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                label="ISBN"
                fullWidth
                {...register('isbn')}
                error={!!errors.isbn}
                helperText={errors.isbn?.message}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Author"
                fullWidth
                {...register('author')}
                error={!!errors.author}
                helperText={errors.author?.message}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                select
                label="Category"
                fullWidth
                defaultValue={editing?.categoryId ?? ''}
                {...register('categoryId')}
                error={!!errors.categoryId}
                helperText={errors.categoryId?.message}
              >
                <MenuItem value="">Select a category</MenuItem>
                {categories.map((c) => (
                  <MenuItem key={c.id} value={c.id}>
                    {c.name}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="Publisher" fullWidth {...register('publisher')} />
            </Grid>
            <Grid item xs={6} sm={2}>
              <TextField
                label="Total Copies"
                fullWidth
                type="number"
                {...register('totalCopies')}
                error={!!errors.totalCopies}
                helperText={errors.totalCopies?.message}
              />
            </Grid>
            <Grid item xs={6} sm={2}>
              <TextField label="Rack No." fullWidth {...register('rackNumber')} />
            </Grid>
            <Grid item xs={6} sm={2}>
              <TextField
                label="Price (INR)"
                fullWidth
                type="number"
                {...register('price')}
                error={!!errors.price}
                helperText={errors.price?.message}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onClose} color="inherit" disabled={saving}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={saving}
            startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}
          >
            Save
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

export default BookFormDialog;
