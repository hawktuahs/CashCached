import { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Skeleton } from '@/components/ui/skeleton'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { 
  Calendar, 
  Shield, 
  Edit3,
  Save,
  X
} from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { useI18n } from '@/context/I18nContext'
import { api } from '@/lib/api'

const profileSchema = z.object({
  firstName: z.string().min(2, 'First name must be at least 2 characters'),
  lastName: z.string().min(2, 'Last name must be at least 2 characters'),
  email: z.string().email('Please enter a valid email address'),
  phone: z.string().optional(),
  address: z.string().optional(),
  dateOfBirth: z.string().optional(),
})

type ProfileFormData = z.infer<typeof profileSchema>

interface CustomerProfile {
  id: string
  firstName: string
  lastName: string
  email: string
  phone?: string
  address?: string
  dateOfBirth?: string
  role: string
  createdAt: string
  lastLoginAt?: string
}

export function CustomerProfile() {
  const { } = useAuth()
  const { t } = useI18n()
  const [profile, setProfile] = useState<CustomerProfile | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isEditing, setIsEditing] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [twoFactorEnabled, setTwoFactorEnabled] = useState<boolean>(false)
  const [isToggling2fa, setIsToggling2fa] = useState(false)

  const form = useForm<ProfileFormData>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      address: '',
      dateOfBirth: '',
    },
  })

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const response = await api.get('/api/customer/profile')
        const profileData = response.data
        setProfile(profileData)
        form.reset({
          firstName: profileData.firstName,
          lastName: profileData.lastName,
          email: profileData.email,
          phone: profileData.phone || '',
          address: profileData.address || '',
          dateOfBirth: profileData.dateOfBirth || '',
        })
      } catch (error) {
        console.error('Failed to fetch profile:', error)
        toast.error('Failed to load profile data')
      } finally {
        setIsLoading(false)
      }
    }

    const fetch2fa = async () => {
      try {
        const res = await api.get('/api/customer/security/2fa')
        const enabled = Boolean(res?.data?.enabled ?? false)
        setTwoFactorEnabled(enabled)
      } catch {}
    }

    fetchProfile()
    fetch2fa()
  }, [form])

  const onSubmit = async (data: ProfileFormData) => {
    setIsSaving(true)
    try {
      const payload = {
        fullName: `${data.firstName} ${data.lastName}`.trim(),
        email: data.email,
        phoneNumber: data.phone || ''
      }
      const response = await api.put('/api/customer/update', payload)
      setProfile(response.data)
      setIsEditing(false)
      toast.success('Profile updated successfully')
    } catch (error) {
      toast.error('Failed to update profile')
    } finally {
      setIsSaving(false)
    }
  }

  const handleCancel = () => {
    form.reset({
      firstName: profile?.firstName || '',
      lastName: profile?.lastName || '',
      email: profile?.email || '',
      phone: profile?.phone || '',
      address: profile?.address || '',
      dateOfBirth: profile?.dateOfBirth || '',
    })
    setIsEditing(false)
  }

  const toggle2fa = async () => {
    setIsToggling2fa(true)
    try {
      if (twoFactorEnabled) {
        await api.put('/api/customer/security/2fa/disable')
        setTwoFactorEnabled(false)
        toast.success('Two-factor disabled')
      } else {
        await api.put('/api/customer/security/2fa/enable')
        setTwoFactorEnabled(true)
        toast.success('Two-factor enabled')
      }
    } catch (e: any) {
      const msg = e?.response?.data?.message || 'Failed to update 2FA settings'
      toast.error(String(msg))
    } finally {
      setIsToggling2fa(false)
    }
  }

  const changePassword = async () => {
    const currentPassword = window.prompt('Enter current password') || ''
    if (!currentPassword) return
    const newPassword = window.prompt('Enter new password') || ''
    if (!newPassword) return
    try {
      await api.post('/api/customer/password/change', { currentPassword, newPassword })
      toast.success('Password changed successfully')
    } catch (e: any) {
      const msg = e?.response?.data?.message || 'Failed to change password'
      toast.error(String(msg))
    }
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <Skeleton className="h-8 w-48" />
        </div>
        <div className="grid gap-6 md:grid-cols-3">
          <div className="md:col-span-1">
            <Skeleton className="h-96 w-full" />
          </div>
          <div className="md:col-span-2">
            <Skeleton className="h-96 w-full" />
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{t('profile.title')}</h1>
          <p className="text-muted-foreground">
            {t('profile.subtitle')}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="outline">{profile?.role?.replace('_', ' ')}</Badge>
          {!isEditing ? (
            <Button onClick={() => setIsEditing(true)}>
              <Edit3 className="mr-2 h-4 w-4" />
              {t('profile.edit')}
            </Button>
          ) : (
            <div className="flex gap-2">
              <Button variant="outline" onClick={handleCancel}>
                <X className="mr-2 h-4 w-4" />
                {t('profile.cancel')}
              </Button>
              <Button 
                onClick={form.handleSubmit(onSubmit)} 
                disabled={isSaving}
              >
                <Save className="mr-2 h-4 w-4" />
                {isSaving ? t('profile.saving') : t('profile.saveChanges')}
              </Button>
            </div>
          )}
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-3">
        <div className="md:col-span-1">
          <Card>
            <CardHeader className="text-center">
              <Avatar className="mx-auto h-24 w-24">
                <AvatarImage src="" alt={profile?.firstName} />
                <AvatarFallback className="text-2xl">
                  {profile?.firstName?.[0]}{profile?.lastName?.[0]}
                </AvatarFallback>
              </Avatar>
              <CardTitle className="text-xl">
                {profile?.firstName} {profile?.lastName}
              </CardTitle>
              <CardDescription>{profile?.email}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-3 text-sm">
                <Shield className="h-4 w-4 text-muted-foreground" />
                <span className="text-muted-foreground">{t('profile.status')}</span>
                <Badge variant="secondary">{t('profile.status.active')}</Badge>
              </div>
              <Separator />
              <div className="space-y-2 text-sm">
                <div className="flex items-center gap-3">
                  <Calendar className="h-4 w-4 text-muted-foreground" />
                  <span className="text-muted-foreground">{t('profile.memberSince')}</span>
                  <span>{new Date(profile?.createdAt || '').toLocaleDateString()}</span>
                </div>
                {profile?.lastLoginAt && (
                  <div className="flex items-center gap-3">
                    <Calendar className="h-4 w-4 text-muted-foreground" />
                    <span className="text-muted-foreground">{t('profile.lastLogin')}</span>
                    <span>{new Date(profile.lastLoginAt).toLocaleDateString()}</span>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="md:col-span-2">
          <Tabs defaultValue="personal" className="w-full">
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="personal">{t('profile.tabs.personal')}</TabsTrigger>
              <TabsTrigger value="security">{t('profile.tabs.security')}</TabsTrigger>
            </TabsList>
            
            <TabsContent value="personal" className="space-y-4">
              <Card>
                <CardHeader>
                  <CardTitle>{t('profile.section.personal.title')}</CardTitle>
                  <CardDescription>
                    {t('profile.section.personal.desc')}
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                      <div className="grid grid-cols-2 gap-4">
                        <FormField
                          control={form.control}
                          name="firstName"
                          render={({ field }) => (
                            <FormItem>
                              <FormLabel>{t('profile.field.firstName')}</FormLabel>
                              <FormControl>
                                <Input
                                  {...field}
                                  disabled={!isEditing}
                                  placeholder={t('profile.placeholder.firstName')}
                                />
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                        <FormField
                          control={form.control}
                          name="lastName"
                          render={({ field }) => (
                            <FormItem>
                              <FormLabel>{t('profile.field.lastName')}</FormLabel>
                              <FormControl>
                                <Input
                                  {...field}
                                  disabled={!isEditing}
                                  placeholder={t('profile.placeholder.lastName')}
                                />
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                      </div>
                      
                      <FormField
                        control={form.control}
                        name="email"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>{t('profile.field.email')}</FormLabel>
                            <FormControl>
                              <Input
                                type="email"
                                {...field}
                                disabled={!isEditing}
                                placeholder={t('profile.placeholder.email')}
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      
                      <FormField
                        control={form.control}
                        name="phone"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>{t('profile.field.phone')}</FormLabel>
                            <FormControl>
                              <Input
                                {...field}
                                disabled={!isEditing}
                                placeholder={t('profile.placeholder.phone')}
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      
                      <FormField
                        control={form.control}
                        name="dateOfBirth"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>{t('profile.field.dob')}</FormLabel>
                            <FormControl>
                              <Input
                                type="date"
                                {...field}
                                disabled={!isEditing}
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      
                      <FormField
                        control={form.control}
                        name="address"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>{t('profile.field.address')}</FormLabel>
                            <FormControl>
                              <Input
                                {...field}
                                disabled={!isEditing}
                                placeholder={t('profile.placeholder.address')}
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </form>
                  </Form>
                </CardContent>
              </Card>
            </TabsContent>
            
            <TabsContent value="security" className="space-y-4">
              <Card>
                <CardHeader>
                  <CardTitle>{t('profile.section.security.title')}</CardTitle>
                  <CardDescription>
                    {t('profile.section.security.desc')}
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="flex items-center justify-between p-4 border rounded-lg">
                    <div className="space-y-1">
                      <h4 className="text-sm font-medium">{t('profile.security.2fa.title')}</h4>
                      <p className="text-sm text-muted-foreground">
                        {t('profile.security.2fa.desc')}
                      </p>
                    </div>
                    <Button variant="outline" size="sm" onClick={toggle2fa} disabled={isToggling2fa}>
                      {twoFactorEnabled ? 'Disable' : t('profile.security.2fa.enable')}
                    </Button>
                  </div>
                  
                  <div className="flex items-center justify-between p-4 border rounded-lg">
                    <div className="space-y-1">
                      <h4 className="text-sm font-medium">{t('profile.security.changePassword.title')}</h4>
                      <p className="text-sm text-muted-foreground">
                        {t('profile.security.changePassword.desc')}
                      </p>
                    </div>
                    <Button variant="outline" size="sm" onClick={changePassword}>
                      {t('profile.security.changePassword.change')}
                    </Button>
                  </div>
                  
                  <div className="flex items-center justify-between p-4 border rounded-lg">
                    <div className="space-y-1">
                      <h4 className="text-sm font-medium">{t('profile.security.loginActivity.title')}</h4>
                      <p className="text-sm text-muted-foreground">
                        {t('profile.security.loginActivity.desc')}
                      </p>
                    </div>
                    <Button variant="outline" size="sm">
                      {t('profile.security.loginActivity.view')}
                    </Button>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </div>
  )
}
