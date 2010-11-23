package org.jboss.arquillian.container.weld.se.embedded_1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.enterprise.inject.spi.Extension;

import org.jboss.arquillian.container.weld.se.embedded_1.shrinkwrap.ShrinkwrapBeanDeploymentArchive;
import org.jboss.arquillian.protocol.local.LocalMethodExecutor;
import org.jboss.arquillian.spi.Configuration;
import org.jboss.arquillian.spi.ContainerMethodExecutor;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.DeployableContainer;
import org.jboss.arquillian.spi.DeploymentException;
import org.jboss.arquillian.spi.LifecycleException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.context.api.helpers.ConcurrentHashMapBeanStore;
import org.jboss.weld.extensions.util.service.ServiceLoader;
import org.jboss.weld.manager.api.WeldManager;

/**
 * WeldSEContainer
 * 
 * @author <a href="mailto:aslak@conduct.no">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class WeldSEContainer implements DeployableContainer
{
   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.arquillian.spi.DeployableContainer#setup(org.jboss.arquillian.spi.Context,
    * org.jboss.arquillian.spi.Configuration)
    */
   public void setup(final Context context, final Configuration configuration)
   {
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.arquillian.spi.DeployableContainer#start(org.jboss.arquillian.spi.Context)
    */
   public void start(final Context context) throws LifecycleException
   {
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.arquillian.spi.DeployableContainer#stop(org.jboss.arquillian.spi.Context)
    */
   public void stop(final Context context) throws LifecycleException
   {
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.arquillian.spi.DeployableContainer#deploy(org.jboss.arquillian.spi.Context,
    * org.jboss.shrinkwrap.api.Archive)
    */
   public ContainerMethodExecutor deploy(final Context context, final Archive<?> archive)
            throws DeploymentException
   {
      final ShrinkwrapBeanDeploymentArchive beanArchive = archive.as(ShrinkwrapBeanDeploymentArchive.class);

      final Deployment deployment = new Deployment()
      {
         public Collection<BeanDeploymentArchive> getBeanDeploymentArchives()
         {
            return Arrays.asList((BeanDeploymentArchive) beanArchive);
         }

         public ServiceRegistry getServices()
         {
            return beanArchive.getServices();
         }

         public BeanDeploymentArchive loadBeanDeploymentArchive(
                  final Class<?> beanClass)
         {
            return beanArchive;
         }

         /*
          * (non-Javadoc)
          * 
          * @see org.jboss.weld.bootstrap.spi.Deployment#getExtensions()
          */
         public Iterable<Metadata<Extension>> getExtensions()
         {
            return transform(ServiceLoader.load(Extension.class, beanArchive.getClassLoader()));
         }
      };

      ContextClassLoaderManager classLoaderManager = new ContextClassLoaderManager(beanArchive.getClassLoader());
      classLoaderManager.enable();

      context.add(ContextClassLoaderManager.class, classLoaderManager);

      WeldBootstrap bootstrap = new WeldBootstrap();
      beanArchive.setBootstrap(bootstrap);

      bootstrap.startContainer(Environments.SE, deployment, new ConcurrentHashMapBeanStore())
                  .startInitialization()
                  .deployBeans()
                  .validateBeans()
                  .endInitialization();

      WeldManager manager = bootstrap.getManager(beanArchive);

      context.add(WeldBootstrap.class, bootstrap);
      context.add(WeldManager.class, manager);

      return new LocalMethodExecutor();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.arquillian.spi.DeployableContainer#undeploy(org.jboss.arquillian.spi.Context,
    * org.jboss.shrinkwrap.api.Archive)
    */
   public void undeploy(final Context context, final Archive<?> archive) throws DeploymentException
   {
      WeldBootstrap bootstrap = context.get(WeldBootstrap.class);
      if (bootstrap != null)
      {
         bootstrap.shutdown();
      }
      ContextClassLoaderManager classLoaderManager = context.get(ContextClassLoaderManager.class);
      classLoaderManager.disable();
   }

   public static Iterable<Metadata<Extension>> transform(final ServiceLoader<Extension> serviceLoader)
   {
      List<Metadata<Extension>> result = new ArrayList<Metadata<Extension>>();
      for (final Extension extension : serviceLoader)
      {
         result.add(new Metadata<Extension>()
         {

            public String getLocation()
            {
               return "unknown";
            }

            public Extension getValue()
            {
               return extension;
            }

         });
      }
      return result;
   }
}