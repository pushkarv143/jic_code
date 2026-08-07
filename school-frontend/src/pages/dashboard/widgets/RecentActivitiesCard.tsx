import Card from '@mui/material/Card';
import CardHeader from '@mui/material/CardHeader';
import CardContent from '@mui/material/CardContent';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import CampaignOutlinedIcon from '@mui/icons-material/CampaignOutlined';
import EmptyState from '@/components/common/EmptyState';

export interface ActivityItem {
  title: string;
  detail: string;
  time: string;
}

export interface RecentActivitiesCardProps {
  activities: ActivityItem[];
  title?: string;
  subheader?: string;
}

/**
 * Simple activity timeline built with plain Box/flex (no @mui/lab dependency,
 * keeping the dependency surface exactly to what's declared in package.json).
 * Despite the generic name, dashboards feed this with whatever "recent items"
 * list makes sense for the role (recent notices, recent payments, etc.).
 */
export function RecentActivitiesCard({
  activities,
  title = 'Recent Activities',
  subheader = 'Latest actions across the school',
}: RecentActivitiesCardProps) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardHeader title={title} subheader={subheader} />
      <CardContent sx={{ pt: 0 }}>
        {activities.length === 0 ? (
          <EmptyState
            icon={<CampaignOutlinedIcon fontSize="large" />}
            title="Nothing recent"
            description="There's nothing new to show here yet."
          />
        ) : (
        <Box>
          {activities.map((activity, idx) => (
            <Box key={idx} sx={{ display: 'flex', gap: 1.5 }}>
              <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', width: 12 }}>
                <Box
                  sx={{
                    width: 10,
                    height: 10,
                    borderRadius: '50%',
                    bgcolor: idx === 0 ? 'primary.main' : 'action.disabled',
                    mt: 0.6,
                    flexShrink: 0,
                  }}
                />
                {idx < activities.length - 1 && (
                  <Box sx={{ width: '1.5px', flexGrow: 1, bgcolor: 'divider', my: 0.5 }} />
                )}
              </Box>
              <Box sx={{ pb: 2.5, flexGrow: 1 }}>
                <Typography variant="body2" fontWeight={600}>
                  {activity.title}
                </Typography>
                <Typography variant="caption" color="text.secondary" display="block">
                  {activity.detail}
                </Typography>
                <Typography variant="caption" color="text.disabled">
                  {activity.time}
                </Typography>
              </Box>
            </Box>
          ))}
        </Box>
        )}
      </CardContent>
    </Card>
  );
}

export default RecentActivitiesCard;
