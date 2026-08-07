import * as yup from 'yup';
import dayjs from 'dayjs';

export const LEAVE_TYPE_OPTIONS = ['SICK', 'CASUAL', 'EARNED', 'MATERNITY', 'PATERNITY', 'OTHER'] as const;

export const leaveApplicationSchema = yup.object({
  leaveType: yup.string().required('Leave type is required'),
  startDate: yup
    .mixed<dayjs.Dayjs>()
    .required('Start date is required')
    .test('is-valid', 'Enter a valid start date', (value) => !!value && dayjs(value).isValid()),
  endDate: yup
    .mixed<dayjs.Dayjs>()
    .required('End date is required')
    .test('is-valid', 'Enter a valid end date', (value) => !!value && dayjs(value).isValid())
    .test('after-start', 'End date must be on or after the start date', function afterStart(value) {
      const { startDate } = this.parent as { startDate?: dayjs.Dayjs };
      if (!value || !startDate) return true;
      return dayjs(value).isSame(startDate, 'day') || dayjs(value).isAfter(startDate, 'day');
    }),
  reason: yup.string().required('Reason is required').max(500),
});

export type LeaveApplicationFormValues = yup.InferType<typeof leaveApplicationSchema>;
